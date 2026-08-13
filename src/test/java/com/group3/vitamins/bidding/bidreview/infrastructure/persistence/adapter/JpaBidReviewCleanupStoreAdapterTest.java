package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocument;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocumentStatus;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewStatus;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewDocumentJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewOutboxJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewDocumentJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewOutboxJpaRepository;
import com.group3.vitamins.file.application.port.FileStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bid-review-cleanup-store;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaBidReviewCleanupStoreAdapter.class,
        JpaBidReviewCleanupStoreAdapterTest.TestConfig.class
})
@DisplayName("JpaBidReviewCleanupStoreAdapter 정리 Outbox 점유·실행·실패")
class JpaBidReviewCleanupStoreAdapterTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long NOTICE_ID = 1L;
    private static final String USER_ID = "EMP001";
    private static final String PROMPT = "재정 상태를 검토해줘.";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 13, 12, 0);

    @Autowired
    private JpaBidReviewCleanupStoreAdapter cleanupAdapter;

    @Autowired
    private BidReviewJpaRepository reviewRepository;

    @Autowired
    private BidReviewDocumentJpaRepository documentRepository;

    @Autowired
    private BidReviewOutboxJpaRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileStoragePort fileStoragePort;

    @BeforeEach
    void resetMock() {
        reset(fileStoragePort);
    }

    @Test
    @DisplayName("CLEANUP 이벤트만 점유하고 REQUESTED 이벤트는 건드리지 않는다")
    void claimsOnlyCleanupEvents() {
        Long reviewId = seedCompletedReview();
        seedOutbox(reviewId, "BID_REVIEW_REQUESTED", "req-1", requestedPayload(reviewId));
        Long cleanupOutboxId = seedOutbox(
                reviewId, "BID_REVIEW_CLEANUP_REQUESTED", "expire-" + reviewId, cleanupPayload(reviewId)
        );

        Optional<Long> claimed = cleanupAdapter.claimNext("server-1", NOW, NOW.plusMinutes(5));

        assertThat(claimed).contains(cleanupOutboxId);

        BidReviewOutboxJpaEntity untouched = outboxRepository.findAll().stream()
                .filter(outbox -> "BID_REVIEW_REQUESTED".equals(outbox.getEventType()))
                .findFirst().orElseThrow();
        assertThat(untouched.getLockOwner()).isNull();
    }

    @Test
    @DisplayName("정리를 실행하면 임시 S3 객체를 지우고 문서를 DELETED, 검토를 EXPIRED로 전환한다")
    void executesCleanupSuccessfully() {
        Long reviewId = seedCompletedReview();
        seedAttachmentDocument(reviewId, 31L, "tmp/reviews/" + reviewId + "/31.pdf");
        Long outboxId = seedOutbox(
                reviewId, "BID_REVIEW_CLEANUP_REQUESTED", "expire-" + reviewId, cleanupPayload(reviewId)
        );
        // execute()의 markPublished는 "이 서버가 점유 중"인 행만 완료 처리하므로, claimNext가 하는
        // 잠금을 먼저 걸어둬야 한다 — 안 그러면 markPublished가 조용히 false를 반환해 예외로 이어진다.
        outboxRepository.findById(outboxId)
                .ifPresent(outbox -> outbox.claim("server-1", NOW.plusMinutes(5), NOW));

        cleanupAdapter.execute(outboxId, "server-1", NOW);

        verify(fileStoragePort).deleteObjects(List.of("tmp/reviews/" + reviewId + "/31.pdf"));

        BidReviewDocumentJpaEntity document = documentRepository
                .findByReviewIdAndBidAttachmentId(reviewId, 31L).orElseThrow();
        assertThat(document.getProcessingStatus()).isEqualTo(BidReviewDocumentStatus.DELETED);
        assertThat(document.getDeletedAt()).isEqualTo(NOW);

        BidReviewJpaEntity review = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(review.getReviewStatus()).isEqualTo(BidReviewStatus.EXPIRED);
        assertThat(review.getCleanupCompletedAt()).isEqualTo(NOW);

        BidReviewOutboxJpaEntity outbox = outboxRepository.findById(outboxId).orElseThrow();
        assertThat(outbox.getPublishStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("이미 프로젝트로 귀속된 검토는 정리를 실행하면 예외가 나고 임시 파일도 지우지 않는다")
    void doesNotExpireAttributedReview() {
        Long reviewId = seedCompletedReview();
        attributeToProject(reviewId, 999L);
        seedAttachmentDocument(reviewId, 31L, "tmp/reviews/" + reviewId + "/31.pdf");
        Long outboxId = seedOutbox(
                reviewId, "BID_REVIEW_CLEANUP_REQUESTED", "expire-" + reviewId, cleanupPayload(reviewId)
        );
        outboxRepository.findById(outboxId)
                .ifPresent(outbox -> outbox.claim("server-1", NOW.plusMinutes(5), NOW));

        assertThatThrownBy(() -> cleanupAdapter.execute(outboxId, "server-1", NOW))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(fileStoragePort);
    }

    @Test
    @DisplayName("이 서버가 점유하지 않은 Outbox는 실행 전에 거부하고 임시 파일도 지우지 않는다")
    void rejectsExecutionWhenNotOwned() {
        Long reviewId = seedCompletedReview();
        seedAttachmentDocument(reviewId, 31L, "tmp/reviews/" + reviewId + "/31.pdf");
        Long outboxId = seedOutbox(
                reviewId, "BID_REVIEW_CLEANUP_REQUESTED", "expire-" + reviewId, cleanupPayload(reviewId)
        );
        // claim을 안 해서 lockOwner가 비어있는 상태 — execute가 삭제 전에 이걸 먼저 막아야 한다.

        assertThatThrownBy(() -> cleanupAdapter.execute(outboxId, "server-1", NOW))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(fileStoragePort);
    }

    @Test
    @DisplayName("실패를 기록하면 재시도 지연시각과 함께 잠금을 해제한다")
    void marksFailed() {
        Long reviewId = seedCompletedReview();
        Long outboxId = seedOutbox(
                reviewId, "BID_REVIEW_CLEANUP_REQUESTED", "expire-" + reviewId, cleanupPayload(reviewId)
        );
        outboxRepository.findById(outboxId).ifPresent(outbox -> outbox.claim("server-1", NOW.plusMinutes(5), NOW));

        cleanupAdapter.markFailed(outboxId, "server-1", NOW.plusMinutes(1), NOW);

        BidReviewOutboxJpaEntity outbox = outboxRepository.findById(outboxId).orElseThrow();
        assertThat(outbox.getPublishStatus()).isEqualTo("PENDING");
        assertThat(outbox.getLockOwner()).isNull();
        assertThat(outbox.getAvailableAt()).isEqualTo(NOW.plusMinutes(1));
    }

    private Long seedCompletedReview() {
        BidReview pending = BidReview.createPending(
                COMPANY_ID, NOTICE_ID, USER_ID, PROMPT, UUID.randomUUID().toString(), NOW.minusHours(5)
        );
        BidReview completed = pending.complete("검토 결과", NOW.minusHours(4), null);
        return reviewRepository.saveAndFlush(BidReviewJpaEntity.from(completed)).getReviewId();
    }

    private void attributeToProject(Long reviewId, Long projectId) {
        BidReviewJpaEntity entity = reviewRepository.findById(reviewId).orElseThrow();
        BidReview current = entity.toDomain();
        BidReview attributed = new BidReview(
                current.reviewId(), current.companyId(), current.noticeId(), current.requestedBy(),
                projectId, current.prompt(), current.reviewStatus(), current.processingAttemptId(),
                current.retryCount(), current.result(), current.errorCode(), current.errorMessage(),
                current.completedAt(), current.expiresAt(), current.abandonedAt(),
                current.cleanupStartedAt(), current.cleanupCompletedAt(),
                current.createdAt(), current.updatedAt()
        );
        entity.apply(attributed);
        reviewRepository.saveAndFlush(entity);
    }

    private void seedAttachmentDocument(Long reviewId, Long attachmentId, String storageKey) {
        BidReviewDocument document = BidReviewDocument
                .createBidAttachment(attachmentId, "제안요청서.pdf", NOW.minusHours(4))
                .ready(storageKey, 204800L, "application/pdf", NOW.minusHours(4));
        documentRepository.saveAndFlush(BidReviewDocumentJpaEntity.from(reviewId, document));
    }

    private Long seedOutbox(Long reviewId, String eventType, String attemptId, com.fasterxml.jackson.databind.JsonNode payload) {
        return outboxRepository.saveAndFlush(BidReviewOutboxJpaEntity.pending(
                UUID.randomUUID().toString(), reviewId, attemptId, eventType, payload, NOW.minusMinutes(1)
        )).getOutboxId();
    }

    private com.fasterxml.jackson.databind.JsonNode cleanupPayload(Long reviewId) {
        return objectMapper.createObjectNode()
                .put("reviewId", reviewId)
                .put("companyId", COMPANY_ID);
    }

    private com.fasterxml.jackson.databind.JsonNode requestedPayload(Long reviewId) {
        return objectMapper.createObjectNode()
                .put("reviewId", reviewId)
                .put("companyId", COMPANY_ID)
                .put("attemptId", UUID.randomUUID().toString())
                .put("retryCount", 0);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        FileStoragePort fileStoragePort() {
            return mock(FileStoragePort.class);
        }
    }
}
