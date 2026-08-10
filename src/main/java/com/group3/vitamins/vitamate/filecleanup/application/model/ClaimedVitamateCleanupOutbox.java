package com.group3.vitamins.vitamate.filecleanup.application.model;

import java.util.List;

public record ClaimedVitamateCleanupOutbox(
        Long outboxId,
        Long cleanupJobId,
        String eventId,
        String eventType,
        String cleanupKey,
        String attemptId,
        List<Long> fileVersionIds,
        int retryCount,
        int publishAttemptCount
) {
}