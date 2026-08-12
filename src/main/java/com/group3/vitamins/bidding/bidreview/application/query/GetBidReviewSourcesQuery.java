package com.group3.vitamins.bidding.bidreview.application.query;

public record GetBidReviewSourcesQuery(
        Long noticeId,
        String userId,
        String role
) {
}