package com.group3.vitamins.bidding.bidreview.application.result;

import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;

import java.time.LocalDateTime;

public record AbandonBidReviewResult(
        Long reviewId,
        String reviewStatus,
        LocalDateTime abandonedAt
) {

    public static AbandonBidReviewResult from(BidReview review) {
        return new AbandonBidReviewResult(
                review.reviewId(),
                review.reviewStatus().name(),
                review.abandonedAt()
        );
    }
}
