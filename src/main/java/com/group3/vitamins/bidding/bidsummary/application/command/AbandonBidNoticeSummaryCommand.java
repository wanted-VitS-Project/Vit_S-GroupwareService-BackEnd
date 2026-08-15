package com.group3.vitamins.bidding.bidsummary.application.command;

public record AbandonBidNoticeSummaryCommand(
        Long summaryId,
        String userId,
        String role
) {
}
