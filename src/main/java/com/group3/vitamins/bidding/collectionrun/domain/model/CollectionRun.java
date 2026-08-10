package com.group3.vitamins.bidding.collectionrun.domain.model;

import java.time.LocalDateTime;

public record CollectionRun(
        Long runId,
        Long conditionId,
        CollectionRunConditionSnapshot conditionSnapshot,
        CollectionRunTriggerType triggerType,
        CollectionRunStatus runStatus,
        String processingAttemptId,
        int retryCount,
        LocalDateTime processingStartedAt,
        LocalDateTime leaseExpiresAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        int collectedCount,
        int insertedCount,
        int updatedCount,
        int skippedCount,
        String errorCode,
        String errorMessage,
        String requestedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    // 사용자의 수동 수집 요청을 처리 대기 상태로 생성합니다.
    public static CollectionRun createPending(
            Long conditionId,
            CollectionRunConditionSnapshot conditionSnapshot,
            String requestedBy,
            LocalDateTime now
    ) {
        return new CollectionRun(
                null,
                conditionId,
                conditionSnapshot,
                CollectionRunTriggerType.MANUAL,
                CollectionRunStatus.PENDING,
                null,
                0,
                null,
                null,
                now,
                null,
                0,
                0,
                0,
                0,
                null,
                null,
                requestedBy,
                now,
                now
        );
    }
}
