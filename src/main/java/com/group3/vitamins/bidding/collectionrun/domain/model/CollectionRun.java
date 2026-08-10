package com.group3.vitamins.bidding.collectionrun.domain.model;

import java.time.LocalDateTime;

public record CollectionRun(
        Long runId,
        Long conditionId,
        CollectionRunTriggerType triggerType,
        CollectionRunStatus runStatus,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        int collectedCount,
        int insertedCount,
        int updatedCount,
        int skippedCount,
        String errorMessage,
        String requestedBy,
        LocalDateTime createdAt
) {

    // 사용자의 수동 수집 요청을 처리 대기 상태로 생성합니다.
    public static CollectionRun createPending(
            Long conditionId,
            String requestedBy,
            LocalDateTime now
    ) {
        return new CollectionRun(
                null,
                conditionId,
                CollectionRunTriggerType.MANUAL,
                CollectionRunStatus.PENDING,
                now,
                null,
                0,
                0,
                0,
                0,
                null,
                requestedBy,
                now
        );
    }
}
