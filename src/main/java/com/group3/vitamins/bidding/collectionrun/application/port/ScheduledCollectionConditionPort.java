package com.group3.vitamins.bidding.collectionrun.application.port;

import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledCollectionConditionPort {

    // 실행 시각이 된 자동 수집 조건을 잠금 점유하여 조회합니다.
    List<CollectionCondition> claimDueConditions(
            LocalDateTime now,
            int batchSize
    );

    void advanceSchedule(Long conditionId, LocalDateTime nextRunAt, LocalDateTime updatedAt);

    void recordScheduledRun(
            Long conditionId,
            LocalDateTime scheduledAt,
            LocalDateTime nextRunAt,
            LocalDateTime updatedAt
    );
}
