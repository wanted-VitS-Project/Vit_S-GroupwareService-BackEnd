package com.group3.vitamins.bidding.bidreview.application.query;

public record GetBidReviewDetailQuery(
        Long reviewId,
        String userId,
        String role
) {
}