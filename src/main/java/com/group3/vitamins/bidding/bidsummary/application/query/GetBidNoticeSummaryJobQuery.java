package com.group3.vitamins.bidding.bidsummary.application.query;

public record GetBidNoticeSummaryJobQuery(
        Long summaryId,
        String attemptId
) {
}