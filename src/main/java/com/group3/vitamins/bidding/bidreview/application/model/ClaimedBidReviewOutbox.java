package com.group3.vitamins.bidding.bidreview.application.model;

// BID_REVIEW_REQUESTED만 발행 대상이다. BID_REVIEW_CLEANUP_REQUESTED는 아직 소비자(정리 Worker)가
// 없어 claim 쿼리에서 제외한다 — 소비자를 만들 때 필터를 넓히면 DB에 쌓여있던 요청이 그대로 발행된다.
public record ClaimedBidReviewOutbox(
        Long outboxId,
        String eventId,
        String eventType,
        Long reviewId,
        Long companyId,
        String attemptId,
        int retryCount,
        int publishAttemptCount
) {
}
