package com.group3.vitamins.bidding.collectionrun.application.port;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTask;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTaskSummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CollectionRunTaskPort {

    // 실행할 외부 요청 조합들을 작업으로 생성합니다.
    void createTasks(Long runId, List<CollectionRequestCombination> combinations);

    // 대기 중이거나 lease가 만료된 작업 하나를 원자적으로 점유합니다.
    Optional<CollectionRunTask> claim(
            Long runId,
            CollectionRequestCombination target,
            String attemptId,
            int retryCount,
            LocalDateTime startedAt,
            LocalDateTime leaseExpiresAt
    );

    // 현재 시도와 일치하는 작업을 처리 건수와 함께 완료합니다.
    boolean complete(
            Long taskId,
            String attemptId,
            int collectedCount,
            int insertedCount,
            int updatedCount,
            int skippedCount,
            LocalDateTime finishedAt
    );

    // 일시 실패한 작업을 다음 재시도가 가능한 상태로 되돌립니다.
    boolean prepareRetry(
            Long taskId,
            String attemptId,
            String errorCode,
            String errorMessage,
            LocalDateTime updatedAt
    );

    // 재시도 한도를 넘긴 작업을 최종 실패로 변경합니다.
    boolean fail(
            Long taskId,
            String attemptId,
            String errorCode,
            String errorMessage,
            LocalDateTime finishedAt
    );

    // 실행에서 다음으로 처리할 대기 또는 lease 만료 작업을 조회합니다.
    Optional<CollectionRunTask> findNextProcessableTask(
            Long runId,
            LocalDateTime now
    );

    // 실행에 속한 모든 요청 조합의 상태와 처리 건수를 집계합니다.
    CollectionRunTaskSummary summarize(Long runId);
}
