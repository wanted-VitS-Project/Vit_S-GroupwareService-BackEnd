package com.group3.vitamins.bidding.bidreview.application.result;

import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;

import java.time.LocalDateTime;

public record CreateBidReviewResult(
        Long reviewId,
        String reviewStatus,
        LocalDateTime requestedAt
) {

    public static CreateBidReviewResult from(BidReview review) {
        return new CreateBidReviewResult(
                review.reviewId(),
                review.reviewStatus().name(),
                review.createdAt()
        );
    }
}