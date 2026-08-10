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
        int publishAttemptCount,
        CollectionRunTaskFailure taskFailure
) {

    // Task DLQ 이벤트인지 확인합니다.
    public boolean isTaskFailureEvent() {
        return taskFailure != null;
    }
}
