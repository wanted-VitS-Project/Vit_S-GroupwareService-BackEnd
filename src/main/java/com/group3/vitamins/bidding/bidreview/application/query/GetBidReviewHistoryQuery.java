package com.group3.vitamins.bidding.bidreview.application.query;

public record GetBidReviewHistoryQuery(
        Long noticeId,
        int page,
        int size,
        String userId,
        String role
) {
}