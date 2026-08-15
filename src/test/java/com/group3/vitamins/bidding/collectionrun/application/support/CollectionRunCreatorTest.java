package com.group3.vitamins.bidding.collectionrun.application.support;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionLookbackPeriod;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunOutboxStorePort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunTaskPort;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRun;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTriggerType;
import com.group3.vitamins.bidding.collectionrun.domain.repository.CollectionRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("CollectionRunCreator 조회 구간 계산")
class CollectionRunCreatorTest {

    private static final Long CONDITION_ID = 1L;
    private static final Long COMPANY_ID = 10L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 12, 0);

    private CollectionRunRepository runRepository;
    private CollectionRunOutboxStorePort outboxStorePort;
    private CollectionRunTaskPort taskPort;
    private CollectionRunCreator creator;

    @BeforeEach
    void setUp() {
        runRepository = mock(CollectionRunRepository.class);
        outboxStorePort = mock(CollectionRunOutboxStorePort.class);
        taskPort = mock(CollectionRunTaskPort.class);

        when(runRepository.save(any(CollectionRun.class)))
                .thenAnswer(invocation -> withRunId(invocation.getArgument(0)));

        creator = new CollectionRunCreator(
                runRepository,
                outboxStorePort,
                taskPort,
                new CollectionRequestCombinationGenerator()
        );
    }

    @Test
    @DisplayName("수동 지정 구간이 없으면 조건의 lookbackPeriod만큼 되돌아간 창을 쓴다")
    void usesConditionLookbackPeriodWhenNoOverride() {
        CollectionCondition condition = conditionWithLookback(CollectionLookbackPeriod.TWO_WEEKS);

        CollectionRun run = creator.create(
                condition, COMPANY_ID, CollectionRunTriggerType.SCHEDULED, null, NOW
        );

        assertThat(run.conditionSnapshot().collectionStartedAt())
                .isEqualTo(NOW.minusDays(14));
        assertThat(run.conditionSnapshot().collectionEndedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("수동 실행이 구간을 직접 지정하면 lookbackPeriod 대신 그 구간을 그대로 쓴다")
    void usesOverrideRangeWhenProvided() {
        CollectionCondition condition = conditionWithLookback(CollectionLookbackPeriod.ONE_MONTH);
        LocalDateTime overrideStartedAt = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime overrideEndedAt = LocalDateTime.of(2026, 7, 31, 0, 0);

        CollectionRun run = creator.create(
                condition, COMPANY_ID, CollectionRunTriggerType.MANUAL, "EMP001", NOW,
                overrideStartedAt, overrideEndedAt
        );

        assertThat(run.conditionSnapshot().collectionStartedAt())
                .isEqualTo(overrideStartedAt);
        assertThat(run.conditionSnapshot().collectionEndedAt())
                .isEqualTo(overrideEndedAt);
    }

    @Test
    @DisplayName("구간을 하나만 지정하면 무시하고 조건의 lookbackPeriod를 쓴다")
    void ignoresPartialOverride() {
        CollectionCondition condition = conditionWithLookback(CollectionLookbackPeriod.ONE_WEEK);

        CollectionRun run = creator.create(
                condition, COMPANY_ID, CollectionRunTriggerType.MANUAL, "EMP001", NOW,
                LocalDateTime.of(2026, 7, 1, 0, 0), null
        );

        assertThat(run.conditionSnapshot().collectionStartedAt())
                .isEqualTo(NOW.minusDays(7));
        assertThat(run.conditionSnapshot().collectionEndedAt()).isEqualTo(NOW);
    }

    private CollectionCondition conditionWithLookback(CollectionLookbackPeriod lookbackPeriod) {
        CollectionCondition condition = mock(CollectionCondition.class);
        when(condition.getConditionId()).thenReturn(CONDITION_ID);
        when(condition.getSourceCode()).thenReturn("NARA");
        when(condition.getConditionName()).thenReturn("테스트 조건");
        when(condition.getNoticeTypes()).thenReturn(List.of(BidNoticeType.SERVICE));
        when(condition.getFilters()).thenReturn(new CollectionConditionFilter(
                List.of("스마트시티"), List.of(), List.of(), null, null, true, null
        ));
        when(condition.getLookbackPeriod()).thenReturn(lookbackPeriod);
        return condition;
    }

    private CollectionRun withRunId(CollectionRun run) {
        return new CollectionRun(
                100L,
                run.conditionId(),
                run.conditionSnapshot(),
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
        );
    }
}
