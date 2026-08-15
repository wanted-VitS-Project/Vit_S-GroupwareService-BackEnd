package com.group3.vitamins.bidding.bidreview.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BidReview 임시파일 보관 기한(expiresAt) 정책")
class BidReviewTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 13, 10, 0);

    @Test
    @DisplayName("공고 입찰마감일시가 완료 시각보다 미래면 마감일시까지 보관한다")
    void completeExpiresAtNoticeDeadlineWhenInFuture() {
        BidReview review = processingReview();
        LocalDateTime deadline = LocalDateTime.of(2026, 8, 30, 18, 0);

        BidReview completed = review.complete("결과", NOW, deadline);

        assertThat(completed.expiresAt()).isEqualTo(deadline);
    }

    @Test
    @DisplayName("공고 입찰마감일시가 없으면(NULL) 완료 시각 + 3시간으로 보관한다")
    void completeFallsBackToThreeHoursWhenDeadlineMissing() {
        BidReview review = processingReview();

        BidReview completed = review.complete("결과", NOW, null);

        assertThat(completed.expiresAt()).isEqualTo(NOW.plusHours(3));
    }

    @Test
    @DisplayName("공고 입찰마감일시가 완료 시각보다 과거면 완료 시각 + 3시간으로 보관한다")
    void completeFallsBackToThreeHoursWhenDeadlineAlreadyPassed() {
        BidReview review = processingReview();
        LocalDateTime pastDeadline = NOW.minusDays(1);

        BidReview completed = review.complete("결과", NOW, pastDeadline);

        assertThat(completed.expiresAt()).isEqualTo(NOW.plusHours(3));
    }

    @Test
    @DisplayName("실패 처리도 동일한 보관 정책을 따른다")
    void failUsesSameRetentionPolicy() {
        BidReview review = processingReview();
        LocalDateTime deadline = LocalDateTime.of(2026, 8, 30, 18, 0);

        BidReview failed = review.fail("DOWNLOAD_FAILED", "다운로드 실패", NOW, deadline);

        assertThat(failed.expiresAt()).isEqualTo(deadline);
    }

    private BidReview processingReview() {
        BidReview pending = BidReview.createPending(
                10L, 1L, "EMP001", "재정 상태를 검토해줘.", UUID.randomUUID().toString(), NOW
        );
        return pending.startProcessing(NOW);
    }
}
