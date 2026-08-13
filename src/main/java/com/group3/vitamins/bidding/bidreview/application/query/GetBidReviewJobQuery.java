package com.group3.vitamins.bidding.bidreview.application.query;

public record GetBidReviewJobQuery(
        Long reviewId,
        String attemptId
) {
}
