package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidreview.application.model.ClaimedBidReviewOutbox;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewOutboxJpaEntity;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bid-review-outbox;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaBidReviewOutboxStoreAdapter.class,
        JpaBidReviewOutboxStoreAdapterTest.JacksonConfig.class
})
@DisplayName("JpaBidReviewOutboxStoreAdapter Outbox 점유·발행 상태 전이")
class JpaBidReviewOutboxStoreAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 13, 10, 0);

    @Autowired
    private JpaBidReviewOutboxStoreAdapter storeAdapter;

    @Autowired
    private BidReviewOutboxJpaRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("BID_REVIEW_REQUESTED만 점유하고 CLEANUP 이벤트는 건드리지 않는다")
    void claimsOnlyReviewRequestedEvents() {
        seedRequested(71L, "attempt-1");
        seedCleanup(72L);

        List<ClaimedBidReviewOutbox> claimed = storeAdapter.claimPublishable(
                "server-1", 10, NOW, NOW.plusMinutes(5)
        );

        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).reviewId()).isEqualTo(71L);
        assertThat(claimed.get(0).eventType()).isEqualTo("BID_REVIEW_REQUESTED");

        BidReviewOutboxJpaEntity cleanup = outboxRepository.findAll().stream()
                .filter(entity -> "BID_REVIEW_CLEANUP_REQUESTED".equals(entity.getEventType()))
                .findFirst().orElseThrow();
        assertThat(cleanup.getPublishStatus()).isEqualTo("PENDING");
        assertThat(cleanup.getLockOwner()).isNull();
    }

    @Test
    @DisplayName("발행 성공을 기록하면 PUBLISHED로 전환한다")
    void marksPublished() {
        Long outboxId = seedRequested(71L, "attempt-1");
        storeAdapter.claimPublishable("server-1", 10, NOW, NOW.plusMinutes(5));

        storeAdapter.markPublished(outboxId, "server-1", NOW.plusSeconds(1));

        BidReviewOutboxJpaEntity entity = outboxRepository.findById(outboxId).orElseThrow();
        assertThat(entity.getPublishStatus()).isEqualTo("PUBLISHED");
        assertThat(entity.getLockOwner()).isNull();
    }

    @Test
    @DisplayName("발행 실패를 기록하면 재시도 대기 시각을 반영하고 잠금을 해제한다")
    void marksPublishFailed() {
        Long outboxId = seedRequested(71L, "attempt-1");
        storeAdapter.claimPublishable("server-1", 10, NOW, NOW.plusMinutes(5));

        storeAdapter.markPublishFailed(
                outboxId, "server-1", "REDIS_PUBLISH_FAILED", NOW.plusSeconds(10), NOW.plusSeconds(1)
        );

        BidReviewOutboxJpaEntity entity = outboxRepository.findById(outboxId).orElseThrow();
        assertThat(entity.getPublishStatus()).isEqualTo("PENDING");
        assertThat(entity.getLockOwner()).isNull();
        assertThat(entity.getAvailableAt()).isEqualTo(NOW.plusSeconds(10));
    }

    private Long seedRequested(Long reviewId, String attemptId) {
        var payload = objectMapper.createObjectNode()
                .put("reviewId", reviewId)
                .put("companyId", 10L)
                .put("attemptId", attemptId)
                .put("retryCount", 0);

        return outboxRepository.saveAndFlush(BidReviewOutboxJpaEntity.pending(
                "event-" + reviewId, reviewId, attemptId, "BID_REVIEW_REQUESTED", payload, NOW
        )).getOutboxId();
    }

    private Long seedCleanup(Long reviewId) {
        var payload = objectMapper.createObjectNode()
                .put("reviewId", reviewId)
                .put("companyId", 10L);

        return outboxRepository.saveAndFlush(BidReviewOutboxJpaEntity.pending(
                "event-cleanup-" + reviewId, reviewId, "abandon-" + reviewId,
                "BID_REVIEW_CLEANUP_REQUESTED", payload, NOW
        )).getOutboxId();
    }

    @TestConfiguration
    static class JacksonConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}