package com.group3.vitamins.bidding.collectionrun.application.result;

import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRun;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTriggerType;

import java.time.LocalDateTime;

public record CollectionRunResult(
        Long runId,
        Long conditionId,
        CollectionRunTriggerType triggerType,
        CollectionRunStatus runStatus,
        LocalDateTime collectionStartedAt,
        LocalDateTime collectionEndedAt,
        int collectedCount,
        int insertedCount,
        int updatedCount,
        int skippedCount,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {

    // 수집 실행 도메인 모델을 Application 계층 출력값으로 변환합니다.
    public static CollectionRunResult from(CollectionRun run) {
        return new CollectionRunResult(
                run.runId(),
                run.conditionId(),
                run.triggerType(),
                run.runStatus(),
                run.conditionSnapshot().collectionStartedAt(),
                run.conditionSnapshot().collectionEndedAt(),
                run.collectedCount(),
                run.insertedCount(),
                run.updatedCount(),
                run.skippedCount(),
                run.errorMessage(),
                run.startedAt(),
                run.finishedAt()
        );
    }
}
