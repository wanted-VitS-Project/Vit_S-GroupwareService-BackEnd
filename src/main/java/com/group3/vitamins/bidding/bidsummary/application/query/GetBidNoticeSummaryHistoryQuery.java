package com.group3.vitamins.bidding.bidsummary.application.query;

public record GetBidNoticeSummaryHistoryQuery(
        Long noticeId,
        int page,
        int size,
        String userId,
        String role
) {
}
