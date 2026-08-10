package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.mapper;

import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.entity.CollectionConditionJpaEntity;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRun;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity.CollectionRunJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CollectionRunPersistenceMapper {

    // JPA Entity를 수집 실행 도메인 모델로 복원합니다.
    public CollectionRun toDomain(CollectionRunJpaEntity entity) {
        return new CollectionRun(
                entity.getCrawlRunId(),
                entity.getCrawlCondition().getCrawlConditionId(),
                entity.getTriggerType(),
                entity.getRunStatus(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getCollectedCount(),
                entity.getInsertedCount(),
                entity.getUpdatedCount(),
                entity.getSkippedCount(),
                entity.getErrorMessage(),
                entity.getRequestedBy(),
                entity.getCreatedAt()
        );
    }

    // 수집 실행 도메인 모델을 JPA Entity로 변환합니다.
    public CollectionRunJpaEntity toEntity(
            CollectionRun run,
            CollectionConditionJpaEntity conditionEntity
    ) {
        return new CollectionRunJpaEntity(
                run.runId(),
                conditionEntity,
                run.triggerType(),
                run.runStatus(),
                run.startedAt(),
                run.finishedAt(),
                run.collectedCount(),
                run.insertedCount(),
                run.updatedCount(),
                run.skippedCount(),
                run.errorMessage(),
                run.requestedBy(),
                run.createdAt(),
                null
        );
    }
}