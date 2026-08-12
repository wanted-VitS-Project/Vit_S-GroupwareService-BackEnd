package com.group3.vitamins.bidding.bidsummary.application.model;

public record ClaimedBidNoticeSummaryOutbox(
        Long outboxId,
        String eventId,
        String eventType,
        Long summaryId,
        Long companyId,
        String attemptId,
        int retryCount,
        int publishAttemptCount
) {
}