package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.mapper;

import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.entity.CollectionConditionJpaEntity;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRun;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity.CollectionRunJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CollectionRunPersistenceMapper {

    private final CollectionRunConditionSnapshotJsonMapper snapshotJsonMapper;

    // JPA Entity를 수집 실행 도메인 모델로 복원합니다.
    public CollectionRun toDomain(CollectionRunJpaEntity entity) {
        return new CollectionRun(
                entity.getCrawlRunId(),
                entity.getCrawlCondition().getCrawlConditionId(),
                snapshotJsonMapper.fromJson(entity.getConditionSnapshot()),
                entity.getTriggerType(),
                entity.getRunStatus(),
                entity.getProcessingAttemptId(),
                entity.getRetryCount(),
                entity.getProcessingStartedAt(),
                entity.getLeaseExpiresAt(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getCollectedCount(),
                entity.getInsertedCount(),
                entity.getUpdatedCount(),
                entity.getSkippedCount(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getRequestedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }

    // 수집 실행 도메인 모델을 JPA Entity로 변환합니다.
    public CollectionRunJpaEntity toEntity(
            CollectionRun run,
            CollectionConditionJpaEntity conditionEntity
    ) {
        return CollectionRunJpaEntity.from(new CollectionRunJpaEntity.PersistenceValues(
                run.runId(),
                conditionEntity,
                snapshotJsonMapper.toJson(run.conditionSnapshot()),
                run.triggerType(),
                run.runStatus(),
                run.processingAttemptId(),
                run.retryCount(),
                run.processingStartedAt(),
                run.leaseExpiresAt(),
                run.startedAt(),
                run.finishedAt(),
                run.collectedCount(),
                run.insertedCount(),
                run.updatedCount(),
                run.skippedCount(),
                run.errorCode(),
                run.errorMessage(),
                run.requestedBy(),
                run.createdAt(),
                run.updatedAt(),
                run.deletedAt()
        ));
    }
}
