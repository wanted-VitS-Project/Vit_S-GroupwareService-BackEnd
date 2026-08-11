package com.group3.vitamins.bidding.collectionrun.application.port;

import com.group3.vitamins.bidding.collectionrun.application.model.ClaimedCollectionRun;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CollectionRunStatePort {

    // 대기 중인 실행을 현재 Worker의 처리 시도로 점유합니다.
    Optional<ClaimedCollectionRun> claim(
            Long runId,
            Long companyId,
            String attemptId,
            int retryCount,
            LocalDateTime startedAt,
            LocalDateTime leaseExpiresAt
    );

    // 여러 페이지를 처리하는 동안 작업 점유 시간을 연장합니다.
    boolean renewLease(
            Long runId,
            String attemptId,
            LocalDateTime leaseExpiresAt,
            LocalDateTime updatedAt
    );

    // 현재 처리 시도와 일치할 때만 실행을 완료합니다.
    boolean complete(
            Long runId,
            Long conditionId,
            String attemptId,
            CollectionRunStatus finalStatus,
            int collectedCount,
            int insertedCount,
            int updatedCount,
            int skippedCount,
            LocalDateTime finishedAt
    );

    // 현재 처리 시도와 일치할 때만 실행을 실패 처리합니다.
    boolean fail(
            Long runId,
            String attemptId,
            String errorCode,
            String errorMessage,
            LocalDateTime finishedAt
    );

    // 일시적 장애가 발생한 실행을 다음 Redis 재시도가 점유할 수 있게 반환합니다.
    boolean prepareRetry(
            Long runId,
            String attemptId,
            String errorCode,
            String errorMessage,
            LocalDateTime updatedAt
    );
}
