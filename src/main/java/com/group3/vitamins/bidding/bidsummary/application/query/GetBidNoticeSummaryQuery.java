package com.group3.vitamins.bidding.bidsummary.application.query;

public record GetBidNoticeSummaryQuery(
        Long summaryId,
        String userId,
        String role
) {
}
