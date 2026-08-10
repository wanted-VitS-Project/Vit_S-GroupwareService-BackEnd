package com.group3.vitamins.bidding.collectionrun.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

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
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {

    // 사용자의 수동 수집 요청을 처리 대기 상태로 생성합니다.
    public static CollectionRun createPending(
            Long conditionId,
            CollectionRunConditionSnapshot conditionSnapshot,
            String requestedBy,
            LocalDateTime now
    ) {
        Objects.requireNonNull(conditionSnapshot, "수집 조건 스냅샷은 필수입니다.");
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
                now,
                null
        );
    }

    // Worker가 실행을 점유하면 처리 시도와 lease 정보를 함께 고정합니다.
    public CollectionRun startProcessing(
            String attemptId,
            LocalDateTime processingStartedAt,
            LocalDateTime leaseExpiresAt
    ) {
        Objects.requireNonNull(attemptId, "처리 시도 ID는 필수입니다.");
        Objects.requireNonNull(processingStartedAt, "처리 시작 시각은 필수입니다.");
        Objects.requireNonNull(leaseExpiresAt, "점유 만료 시각은 필수입니다.");
        return copy(
                CollectionRunStatus.PROCESSING,
                attemptId,
                retryCount,
                processingStartedAt,
                leaseExpiresAt,
                startedAt == null ? processingStartedAt : startedAt,
                null,
                collectedCount,
                insertedCount,
                updatedCount,
                skippedCount,
                null,
                null,
                processingStartedAt
        );
    }

    // 수집 결과 집계와 최종 상태를 한 번에 완료 처리합니다.
    public CollectionRun complete(
            CollectionRunStatus finalStatus,
            int collectedCount,
            int insertedCount,
            int updatedCount,
            int skippedCount,
            LocalDateTime finishedAt
    ) {
        if (finalStatus != CollectionRunStatus.COMPLETED
                && finalStatus != CollectionRunStatus.PARTIAL_SUCCESS) {
            throw new IllegalArgumentException("완료 가능한 최종 상태가 아닙니다.");
        }
        return copy(
                finalStatus,
                processingAttemptId,
                retryCount,
                processingStartedAt,
                null,
                startedAt,
                finishedAt,
                collectedCount,
                insertedCount,
                updatedCount,
                skippedCount,
                null,
                null,
                finishedAt
        );
    }

    // 안전한 오류 코드만 남기고 실행을 실패 상태로 종료합니다.
    public CollectionRun fail(
            String errorCode,
            LocalDateTime finishedAt
    ) {
        Objects.requireNonNull(errorCode, "오류 코드는 필수입니다.");
        return copy(
                CollectionRunStatus.FAILED,
                processingAttemptId,
                retryCount,
                processingStartedAt,
                null,
                startedAt,
                finishedAt,
                collectedCount,
                insertedCount,
                updatedCount,
                skippedCount,
                errorCode,
                null,
                finishedAt
        );
    }

    private CollectionRun copy(
            CollectionRunStatus status,
            String attemptId,
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
            LocalDateTime updatedAt
    ) {
        return new CollectionRun(
                runId,
                conditionId,
                conditionSnapshot,
                triggerType,
                status,
                attemptId,
                retryCount,
                processingStartedAt,
                leaseExpiresAt,
                startedAt,
                finishedAt,
                collectedCount,
                insertedCount,
                updatedCount,
                skippedCount,
                errorCode,
                errorMessage,
                requestedBy,
                createdAt,
                updatedAt,
                deletedAt
        );
    }
}
