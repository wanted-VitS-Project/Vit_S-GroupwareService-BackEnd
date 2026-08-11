package com.group3.vitamins.bidding.collectionrun.application.support;

import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunOutbox;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunOutboxStorePort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunTaskPort;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRun;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunConditionSnapshot;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTriggerType;
import com.group3.vitamins.bidding.collectionrun.domain.repository.CollectionRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CollectionRunCreator {

    private static final String COLLECTION_RUN_REQUESTED =
            "COLLECTION_RUN_REQUESTED";
    private static final int INITIAL_COLLECTION_LOOKBACK_DAYS = 1;

    private final CollectionRunRepository runRepository;
    private final CollectionRunOutboxStorePort outboxStorePort;
    private final CollectionRunTaskPort taskPort;
    private final CollectionRequestCombinationGenerator combinationGenerator;

    // 실행, 세부 작업과 발행 대기 Outbox를 같은 트랜잭션에서 생성합니다.
    public CollectionRun create(
            CollectionCondition condition,
            Long companyId,
            CollectionRunTriggerType triggerType,
            String requestedBy,
            LocalDateTime now
    ) {
        Objects.requireNonNull(condition, "수집 조건은 필수입니다.");
        Objects.requireNonNull(companyId, "회사 ID는 필수입니다.");
        Objects.requireNonNull(triggerType, "수집 실행 유형은 필수입니다.");
        Objects.requireNonNull(now, "생성 시각은 필수입니다.");

        LocalDateTime collectionStartedAt = condition.getLastSuccessAt() == null
                ? now.minusDays(INITIAL_COLLECTION_LOOKBACK_DAYS)
                : condition.getLastSuccessAt();

        CollectionRunConditionSnapshot snapshot =
                new CollectionRunConditionSnapshot(
                        condition.getSourceCode(),
                        condition.getConditionName(),
                        condition.getNoticeTypes(),
                        condition.getFilters(),
                        collectionStartedAt,
                        now
                );

        CollectionRun savedRun = runRepository.save(
                CollectionRun.createPending(
                        condition.getConditionId(),
                        snapshot,
                        triggerType,
                        requestedBy,
                        now
                )
        );

        taskPort.createTasks(
                savedRun.runId(),
                combinationGenerator.generate(snapshot)
        );

        outboxStorePort.savePending(
                new CollectionRunOutbox.Pending(
                        UUID.randomUUID().toString(),
                        savedRun.runId(),
                        condition.getConditionId(),
                        companyId,
                        UUID.randomUUID().toString(),
                        COLLECTION_RUN_REQUESTED,
                        0,
                        now
                )
        );

        return savedRun;
    }
}
