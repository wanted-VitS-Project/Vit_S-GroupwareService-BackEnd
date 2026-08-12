package com.group3.vitamins.bidding.bidsummary.application.command;

public record HandleBidNoticeSummaryCallbackCommand(
        Long summaryId,
        String attemptId,
        String summaryStatus,
        String overviewSummary,
        String amountSummary,
        String scheduleSummary,
        String qualificationSummary,
        String taskSummary,
        String riskSummary,
        String errorMessage,
        boolean retryable
) {
}
