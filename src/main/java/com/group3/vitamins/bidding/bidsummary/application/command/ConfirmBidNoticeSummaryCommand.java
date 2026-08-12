package com.group3.vitamins.bidding.bidsummary.application.command;

public record ConfirmBidNoticeSummaryCommand(
        Long summaryId,
        String userId,
        String role
) {
}
