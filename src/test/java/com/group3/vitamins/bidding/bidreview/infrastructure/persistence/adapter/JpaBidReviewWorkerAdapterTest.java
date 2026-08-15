package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewNoticeDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewWorkerPort;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocument;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocumentRole;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewStatus;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewCitationJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewDocumentJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewOutboxJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewCitationJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewDocumentJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewOutboxJpaRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bid-review-worker;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaBidReviewWorkerAdapter.class,
        JpaBidReviewWorkerAdapterTest.JacksonConfig.class
})
@DisplayName("JpaBidReviewWorkerAdapter Worker 작업 조회·진행상황·완료·실패 처리")
class JpaBidReviewWorkerAdapterTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long NOTICE_ID = 1L;
    private static final String USER_ID = "EMP001";
    private static final String PROMPT = "재정 상태와 인력으로 수행 가능한지 검토해줘.";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 13, 10, 0);

    @Autowired
    private JpaBidReviewWorkerAdapter workerAdapter;

    @Autowired
    private BidReviewJpaRepository reviewRepository;

    @Autowired
    private BidReviewDocumentJpaRepository documentRepository;

    @Autowired
    private BidReviewCitationJpaRepository citationRepository;

    @Autowired
    private BidReviewOutboxJpaRepository outboxRepository;

    @Autowired
    private BidReviewNoticeDocumentPort noticeDocumentPort;

    // noticeDocumentPort는 Mockito mock @Bean이라 Spring 테스트 컨텍스트와 함께 테스트 클래스
    // 전체에서 재사용된다 - 한 테스트가 건 스텁이 다음 테스트로 새어나가지 않게 매번 리셋한다.
    @BeforeEach
    void resetNoticeDocumentPortStub() {
        reset(noticeDocumentPort);
    }

    @Test
    @DisplayName("현재 attemptId와 일치하면 작업을 점유하고 PROCESSING으로 전환한다")
    void claimsJobAndStartsProcessing() {
        String attemptId = UUID.randomUUID().toString();
        Long reviewId = seedReview(attemptId);
        seedDocument(reviewId, 31L, null, "제안요청서.pdf");
        seedDocument(reviewId, null, 501L, "원가계산_기준.pdf");

        Optional<BidReviewWorkerPort.ClaimedJob> claimed =
                workerAdapter.claimJob(reviewId, attemptId, NOW);

        assertThat(claimed).isPresent();
        assertThat(claimed.get().companyId()).isEqualTo(COMPANY_ID);
        assertThat(claimed.get().noticeId()).isEqualTo(NOTICE_ID);
        assertThat(claimed.get().documents()).hasSize(2);

        BidReviewJpaEntity persisted = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(persisted.getReviewStatus()).isEqualTo(BidReviewStatus.PROCESSING);
    }

    @Test
    @DisplayName("attemptId가 다르면 작업을 점유하지 않는다")
    void doesNotClaimMismatchedAttempt() {
        Long reviewId = seedReview(UUID.randomUUID().toString());

        Optional<BidReviewWorkerPort.ClaimedJob> claimed =
                workerAdapter.claimJob(reviewId, UUID.randomUUID().toString(), NOW);

        assertThat(claimed).isEmpty();
    }

    @Test
    @DisplayName("PROCESSING callback은 지정한 첨부 문서만 READY로 갱신한다")
    void reportsProgressForAttachment() {
        String attemptId = UUID.randomUUID().toString();
        Long reviewId = seedProcessingReview(attemptId);
        seedDocument(reviewId, 31L, null, "제안요청서.pdf");

        BidReviewWorkerPort.CallbackUpdate update = workerAdapter.reportProgress(
                reviewId,
                attemptId,
                List.of(new BidReviewWorkerPort.DocumentOutcome(
                        31L, "READY", "tmp/reviews/71/31.pdf", 204800L, "application/pdf"
                )),
                NOW
        );

        assertThat(update.exists()).isTrue();
        assertThat(update.accepted()).isTrue();

        BidReviewDocumentJpaEntity document = documentRepository
                .findByReviewIdAndBidAttachmentId(reviewId, 31L)
                .orElseThrow();
        assertThat(document.getProcessingStatus().name()).isEqualTo("READY");
        assertThat(document.getTemporaryStorageKey()).isEqualTo("tmp/reviews/71/31.pdf");
    }

    @Test
    @DisplayName("COMPLETED callback은 결과를 저장하고 문서 역할별로 근거를 연결한다")
    void completesReviewWithCitations() {
        String attemptId = UUID.randomUUID().toString();
        Long reviewId = seedProcessingReview(attemptId);
        seedDocument(reviewId, 31L, null, "제안요청서.pdf");
        seedDocument(reviewId, null, 501L, "원가계산_기준.pdf");

        BidReviewWorkerPort.CallbackUpdate update = workerAdapter.complete(
                reviewId,
                attemptId,
                "재정 상태가 양호합니다.",
                List.of(new BidReviewWorkerPort.DocumentOutcome(
                        31L, "READY", "tmp/reviews/71/31.pdf", 204800L, "application/pdf"
                )),
                List.of(new BidReviewWorkerPort.CitationInput(
                        1, "INTERNAL_REFERENCE", null, 501L, null, "원가계산_기준.pdf", 3, null, "발췌문"
                )),
                NOW
        );

        assertThat(update.accepted()).isTrue();
        assertThat(update.currentStatus()).isEqualTo("COMPLETED");

        BidReviewJpaEntity persisted = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(persisted.getResult()).isEqualTo("재정 상태가 양호합니다.");
        assertThat(persisted.getExpiresAt()).isEqualTo(NOW.plusHours(3));

        List<BidReviewCitationJpaEntity> citations = citationRepository.findAll();
        assertThat(citations).hasSize(1);
        Long referenceDocumentId = documentRepository
                .findByReviewIdAndDocumentRoleAndReferenceFileId(
                        reviewId, BidReviewDocumentRole.INTERNAL_REFERENCE, 501L)
                .orElseThrow()
                .getReviewDocumentId();
        assertThat(citations.get(0).getReviewDocumentId()).isEqualTo(referenceDocumentId);
        assertThat(citations.get(0).getExcerpt()).isEqualTo("발췌문");
    }

    @Test
    @DisplayName("COMPLETED callback은 사내 문서함 참조 근거도 companyDocumentVersionId로 연결한다")
    void completesReviewWithCompanyDocumentCitation() {
        String attemptId = UUID.randomUUID().toString();
        Long reviewId = seedProcessingReview(attemptId);
        seedDocument(reviewId, 31L, null, "제안요청서.pdf");
        seedCompanyDocument(reviewId, 9001L, "재무제표.xlsx");

        BidReviewWorkerPort.CallbackUpdate update = workerAdapter.complete(
                reviewId,
                attemptId,
                "재정 상태가 양호합니다.",
                List.of(new BidReviewWorkerPort.DocumentOutcome(
                        31L, "READY", "tmp/reviews/71/31.pdf", 204800L, "application/pdf"
                )),
                List.of(new BidReviewWorkerPort.CitationInput(
                        1, "COMPANY_DOCUMENT_REFERENCE", null, null, 9001L, "재무제표.xlsx", null, "시트1", "발췌문"
                )),
                NOW
        );

        assertThat(update.accepted()).isTrue();

        List<BidReviewCitationJpaEntity> citations = citationRepository.findAll();
        assertThat(citations).hasSize(1);
        Long companyDocumentId = documentRepository
                .findByReviewIdAndDocumentRoleAndCompanyDocumentVersionId(
                        reviewId, BidReviewDocumentRole.COMPANY_DOCUMENT_REFERENCE, 9001L)
                .orElseThrow()
                .getReviewDocumentId();
        assertThat(citations.get(0).getReviewDocumentId()).isEqualTo(companyDocumentId);
    }

    @Test
    @DisplayName("공고 입찰마감일시가 미래면 완료 시각 + 3시간이 아니라 그 마감일시까지 보관한다")
    void completeExpiresAtNoticeDeadline() {
        String attemptId = UUID.randomUUID().toString();
        Long reviewId = seedProcessingReview(attemptId);
        seedDocument(reviewId, 31L, null, "제안요청서.pdf");
        LocalDateTime deadline = LocalDateTime.of(2026, 8, 30, 18, 0);
        when(noticeDocumentPort.findAccessibleNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(new BidReviewNoticeDocumentPort.NoticeSnapshot(
                        NOTICE_ID, "스마트시티 통합관제 용역", deadline
                )));

        workerAdapter.complete(
                reviewId, attemptId, "재정 상태가 양호합니다.",
                List.of(new BidReviewWorkerPort.DocumentOutcome(
                        31L, "READY", "tmp/reviews/71/31.pdf", 204800L, "application/pdf"
                )),
                null,
                NOW
        );

        BidReviewJpaEntity persisted = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(persisted.getExpiresAt()).isEqualTo(deadline);
    }

    @Test
    @DisplayName("재시도 가능한 FAILED callback은 새 attemptId로 PENDING 복귀하고 지연 Outbox를 저장한다")
    void retriesOnRetryableFailure() {
        String attemptId = UUID.randomUUID().toString();
        Long reviewId = seedProcessingReview(attemptId);

        BidReviewWorkerPort.CallbackUpdate update = workerAdapter.fail(
                reviewId, attemptId, "DOWNLOAD_TIMEOUT", "다운로드 시간 초과", true, null, NOW
        );

        assertThat(update.accepted()).isTrue();
        assertThat(update.currentStatus()).isEqualTo("PENDING");

        BidReviewJpaEntity persisted = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(persisted.getRetryCount()).isEqualTo(1);
        assertThat(persisted.getProcessingAttemptId()).isNotEqualTo(attemptId);

        List<BidReviewOutboxJpaEntity> outboxEntries = outboxRepository.findAll();
        assertThat(outboxEntries).hasSize(1);
        assertThat(outboxEntries.get(0).getAttemptId()).isEqualTo(persisted.getProcessingAttemptId());
        assertThat(outboxEntries.get(0).getAvailableAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    @DisplayName("재시도 불가능한 FAILED callback은 최종 실패로 종료한다")
    void failsFinallyWhenNotRetryable() {
        String attemptId = UUID.randomUUID().toString();
        Long reviewId = seedProcessingReview(attemptId);

        BidReviewWorkerPort.CallbackUpdate update = workerAdapter.fail(
                reviewId, attemptId, "UNSUPPORTED_FORMAT", "지원하지 않는 형식", false, null, NOW
        );

        assertThat(update.currentStatus()).isEqualTo("FAILED");

        BidReviewJpaEntity persisted = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(persisted.getReviewStatus()).isEqualTo(BidReviewStatus.FAILED);
        assertThat(persisted.getExpiresAt()).isEqualTo(NOW.plusHours(3));
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("오래된 attempt의 completed callback은 멱등 거절한다")
    void ignoresStaleCompleteCallback() {
        String attemptId = UUID.randomUUID().toString();
        Long reviewId = seedProcessingReview(attemptId);

        BidReviewWorkerPort.CallbackUpdate update = workerAdapter.complete(
                reviewId, UUID.randomUUID().toString(), "결과", null, null, NOW
        );

        assertThat(update.exists()).isTrue();
        assertThat(update.accepted()).isFalse();
    }

    @Test
    @DisplayName("살아있는 outbox 없이 오래 멈춘 검토를 재큐잉한다")
    void requeuesOrphanedReview() {
        String attemptId = UUID.randomUUID().toString();
        Long reviewId = seedProcessingReview(attemptId);

        int recovered = workerAdapter.recoverOrphaned(NOW.plusMinutes(1), 100, NOW.plusMinutes(11));

        assertThat(recovered).isEqualTo(1);
        BidReviewJpaEntity persisted = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(persisted.getReviewStatus()).isEqualTo(BidReviewStatus.PENDING);
        assertThat(persisted.getRetryCount()).isEqualTo(1);
        assertThat(persisted.getProcessingAttemptId()).isNotEqualTo(attemptId);
        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("재시도를 이미 소진한 고아 검토는 재큐잉 대신 최종 FAILED로 종료한다")
    void failsOrphanedReviewAfterRetryLimit() {
        String attemptId = UUID.randomUUID().toString();
        Long reviewId = seedProcessingReview(attemptId);
        BidReviewJpaEntity entity = reviewRepository.findById(reviewId).orElseThrow();
        BidReview domain = entity.toDomain();
        for (int i = 0; i < 3; i++) {
            domain = domain.retryProcessing(UUID.randomUUID().toString(), "TEMP", "일시 장애", NOW);
        }
        entity.apply(domain);
        reviewRepository.saveAndFlush(entity);

        int recovered = workerAdapter.recoverOrphaned(NOW.plusMinutes(1), 100, NOW.plusMinutes(11));

        assertThat(recovered).isEqualTo(1);
        BidReviewJpaEntity persisted = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(persisted.getReviewStatus()).isEqualTo(BidReviewStatus.FAILED);
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("이미 살아있는 outbox가 있으면 손대지 않는다")
    void skipsReviewThatAlreadyHasPendingOutbox() {
        String attemptId = UUID.randomUUID().toString();
        Long reviewId = seedProcessingReview(attemptId);
        var payload = new ObjectMapper().createObjectNode()
                .put("reviewId", reviewId).put("companyId", COMPANY_ID)
                .put("attemptId", attemptId).put("retryCount", 0);
        outboxRepository.saveAndFlush(BidReviewOutboxJpaEntity.pending(
                UUID.randomUUID().toString(), reviewId, attemptId,
                "BID_REVIEW_REQUESTED", payload, NOW, NOW
        ));

        int recovered = workerAdapter.recoverOrphaned(NOW.plusMinutes(1), 100, NOW.plusMinutes(11));

        assertThat(recovered).isZero();
        BidReviewJpaEntity persisted = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(persisted.getReviewStatus()).isEqualTo(BidReviewStatus.PROCESSING);
    }

    private Long seedReview(String attemptId) {
        BidReview review = BidReview.createPending(
                COMPANY_ID, NOTICE_ID, USER_ID, PROMPT, attemptId, NOW
        );
        return reviewRepository.saveAndFlush(BidReviewJpaEntity.from(review)).getReviewId();
    }

    private Long seedProcessingReview(String attemptId) {
        Long reviewId = seedReview(attemptId);
        BidReviewJpaEntity entity = reviewRepository.findById(reviewId).orElseThrow();
        entity.apply(entity.toDomain().startProcessing(NOW));
        return reviewId;
    }

    private void seedDocument(Long reviewId, Long attachmentId, Long referenceFileId, String fileName) {
        BidReviewDocument document = attachmentId != null
                ? BidReviewDocument.createBidAttachment(attachmentId, fileName, NOW)
                : BidReviewDocument.createInternalReference(referenceFileId, fileName, NOW);
        documentRepository.saveAndFlush(BidReviewDocumentJpaEntity.from(reviewId, document));
    }

    private void seedCompanyDocument(Long reviewId, Long companyDocumentVersionId, String fileName) {
        BidReviewDocument document =
                BidReviewDocument.createCompanyDocumentReference(companyDocumentVersionId, fileName, NOW);
        documentRepository.saveAndFlush(BidReviewDocumentJpaEntity.from(reviewId, document));
    }

    @TestConfiguration
    static class JacksonConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        BidReviewNoticeDocumentPort noticeDocumentPort() {
            // 기본(스텁 없음)은 Optional.empty() - complete()/fail()이 완료·실패 시각 + 3시간
            // fallback으로 처리하므로 기존 테스트들의 만료 시각 검증은 그대로 유지된다.
            return mock(BidReviewNoticeDocumentPort.class);
        }
    }
}
