package com.group3.vitamins.bidding.collectionrun.application.model;

import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTaskStatus;

import java.time.LocalDateTime;

public record CollectionRunTask(
        Long taskId,
        Long runId,
        CollectionRequestCombination target,
        CollectionRunTaskStatus status,
        String attemptId,
        int retryCount,
        LocalDateTime leaseExpiresAt
) {
}
