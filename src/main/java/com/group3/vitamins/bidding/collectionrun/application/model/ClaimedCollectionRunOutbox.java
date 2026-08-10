package com.group3.vitamins.bidding.collectionrun.application.model;

public record ClaimedCollectionRunOutbox(
        Long outboxId,
        Long runId,
        Long conditionId,
        Long companyId,
        String eventId,
        String eventType,
        String attemptId,
        int retryCount,
        int publishAttemptCount
) {
}