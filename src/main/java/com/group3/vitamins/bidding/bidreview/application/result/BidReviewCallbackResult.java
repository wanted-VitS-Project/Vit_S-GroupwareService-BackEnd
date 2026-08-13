package com.group3.vitamins.bidding.bidreview.application.result;

public record BidReviewCallbackResult(
        boolean accepted,
        Long reviewId,
        String reviewStatus,
        String reason
) {
}
