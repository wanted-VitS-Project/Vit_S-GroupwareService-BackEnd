package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewOutboxJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewOutboxJpaRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bid-review-expiry-scan;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaBidReviewExpiryScanAdapter.class,
        JpaBidReviewExpiryScanAdapterTest.JacksonConfig.class
})
@DisplayName("JpaBidReviewExpiryScanAdapter 만료 후보 조회·점유")
class JpaBidReviewExpiryScanAdapterTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long NOTICE_ID = 1L;
    private static final String USER_ID = "EMP001";
    private static final String PROMPT = "재정 상태를 검토해줘.";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 13, 12, 0);

    @Autowired
    private JpaBidReviewExpiryScanAdapter scanAdapter;

    @Autowired
    private BidReviewJpaRepository reviewRepository;

    @Autowired
    private BidReviewOutboxJpaRepository outboxRepository;

    @Test
    @DisplayName("만료 후보만 조회한다 — 미만료·귀속됨·이미 점유됨·부적격 상태는 제외")
    void findsOnlyEligibleExpiredCandidates() {
        Long eligible = seedCompletedReview(NOW.minusHours(4));
        seedCompletedReview(NOW);
        Long attributed = seedCompletedReview(NOW.minusHours(4));
        attributeToProject(attributed, 999L);
        Long alreadyStarted = seedCompletedReview(NOW.minusHours(4));
        markCleanupStarted(alreadyStarted, NOW.minusMinutes(1));
        seedPendingReview();

        List<Long> candidates = scanAdapter.findExpiredCandidateIds(NOW, 10);

        assertThat(candidates).containsExactly(eligible);
    }

    @Test
    @DisplayName("점유에 성공하면 cleanup_started_at을 찍고 정리 Outbox를 저장한다")
    void claimsAndRequestsCleanup() {
        Long reviewId = seedCompletedReview(NOW.minusHours(4));

        boolean claimed = scanAdapter.claimAndRequestCleanup(reviewId, NOW);

        assertThat(claimed).isTrue();
        BidReviewJpaEntity entity = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(entity.getCleanupStartedAt()).isEqualTo(NOW);

        List<BidReviewOutboxJpaEntity> outboxes = outboxRepository.findAll();
        assertThat(outboxes).hasSize(1);
        assertThat(outboxes.get(0).getEventType()).isEqualTo("BID_REVIEW_CLEANUP_REQUESTED");
        assertThat(outboxes.get(0).getAttemptId()).isEqualTo("expire-" + reviewId);
        assertThat(outboxes.get(0).getReviewId()).isEqualTo(reviewId);
    }

    @Test
    @DisplayName("이미 점유된 검토는 다시 점유하지 않는다")
    void doesNotClaimAlreadyStarted() {
        Long reviewId = seedCompletedReview(NOW.minusHours(4));
        markCleanupStarted(reviewId, NOW.minusMinutes(1));

        boolean claimed = scanAdapter.claimAndRequestCleanup(reviewId, NOW);

        assertThat(claimed).isFalse();
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("프로젝트로 귀속된 검토는 점유하지 않는다")
    void doesNotClaimAttributedReview() {
        Long reviewId = seedCompletedReview(NOW.minusHours(4));
        attributeToProject(reviewId, 999L);

        boolean claimed = scanAdapter.claimAndRequestCleanup(reviewId, NOW);

        assertThat(claimed).isFalse();
    }

    @Test
    @DisplayName("아직 만료 시각이 안 됐으면 점유하지 않는다")
    void doesNotClaimNotYetExpiredReview() {
        Long reviewId = seedCompletedReview(NOW);

        boolean claimed = scanAdapter.claimAndRequestCleanup(reviewId, NOW);

        assertThat(claimed).isFalse();
    }

    @Test
    @DisplayName("FAILED로 끝난 검토도 만료 후보에 포함된다")
    void includesFailedReviewsAsCandidates() {
        Long failedEligible = seedFailedReview(NOW.minusHours(4));

        List<Long> candidates = scanAdapter.findExpiredCandidateIds(NOW, 10);

        assertThat(candidates).contains(failedEligible);
    }

    @Test
    @DisplayName("PENDING 검토는 claimAndRequestCleanup으로도 점유하지 않는다")
    void doesNotClaimPendingReview() {
        Long reviewId = seedPendingReview();

        boolean claimed = scanAdapter.claimAndRequestCleanup(reviewId, NOW);

        assertThat(claimed).isFalse();
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    // completedAt을 받아 expiresAt = completedAt + 3시간으로 계산되는 완료 검토를 만든다.
    private Long seedCompletedReview(LocalDateTime completedAt) {
        BidReview pending = BidReview.createPending(
                COMPANY_ID, NOTICE_ID, USER_ID, PROMPT,
                UUID.randomUUID().toString(), completedAt.minusMinutes(1)
        );
        BidReview completed = pending.complete("검토 결과", completedAt, null);
        return reviewRepository.saveAndFlush(BidReviewJpaEntity.from(completed)).getReviewId();
    }

    // failedAt을 받아 expiresAt = failedAt + 3시간으로 계산되는 실패 검토를 만든다.
    private Long seedFailedReview(LocalDateTime failedAt) {
        BidReview pending = BidReview.createPending(
                COMPANY_ID, NOTICE_ID, USER_ID, PROMPT,
                UUID.randomUUID().toString(), failedAt.minusMinutes(1)
        );
        BidReview failed = pending.fail("DOWNLOAD_FAILED", "첨부 다운로드에 실패했습니다.", failedAt, null);
        return reviewRepository.saveAndFlush(BidReviewJpaEntity.from(failed)).getReviewId();
    }

    private Long seedPendingReview() {
        BidReview pending = BidReview.createPending(
                COMPANY_ID, NOTICE_ID, USER_ID, PROMPT, UUID.randomUUID().toString(), NOW
        );
        return reviewRepository.saveAndFlush(BidReviewJpaEntity.from(pending)).getReviewId();
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

    private void markCleanupStarted(Long reviewId, LocalDateTime now) {
        BidReviewJpaEntity entity = reviewRepository.findById(reviewId).orElseThrow();
        entity.apply(entity.toDomain().markCleanupStarted(now));
        reviewRepository.saveAndFlush(entity);
    }

    @TestConfiguration
    static class JacksonConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}
