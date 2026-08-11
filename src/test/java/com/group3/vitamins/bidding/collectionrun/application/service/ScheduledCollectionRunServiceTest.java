package com.group3.vitamins.bidding.collectionrun.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.support.CollectionScheduleCalculator;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionScheduleType;
import com.group3.vitamins.bidding.collectionrun.application.port.ScheduledCollectionConditionPort;
import com.group3.vitamins.bidding.collectionrun.application.support.CollectionRunCreator;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTriggerType;
import com.group3.vitamins.bidding.collectionrun.domain.repository.CollectionRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ScheduledCollectionRunService 자동 수집 실행 생성")
class ScheduledCollectionRunServiceTest {

    private static final Long CONDITION_ID = 20L;
    private static final Long COMPANY_ID = 10L;

    private ScheduledCollectionConditionPort scheduledConditionPort;
    private CollectionRunRepository runRepository;
    private CollectionRunCreator runCreator;
    private ScheduledCollectionRunService service;

    @BeforeEach
    void setUp() {
        scheduledConditionPort = mock(ScheduledCollectionConditionPort.class);
        runRepository = mock(CollectionRunRepository.class);
        runCreator = mock(CollectionRunCreator.class);

        Clock clock = Clock.fixed(
                Instant.parse("2026-08-11T01:00:00Z"),
                ZoneOffset.UTC
        );

        service = new ScheduledCollectionRunService(
                scheduledConditionPort,
                runRepository,
                runCreator,
                new CollectionScheduleCalculator(),
                clock
        );
    }

    @Test
    @DisplayName("실행 시각이 된 조건으로 자동 Run을 만들고 다음 시각을 갱신한다")
    void createsScheduledRunAndAdvancesSchedule() {
        CollectionCondition condition = dueCondition();
        when(scheduledConditionPort.claimDueConditions(any(), eq(50)))
                .thenReturn(List.of(condition));
        when(runRepository.existsActiveByConditionId(CONDITION_ID))
                .thenReturn(false);

        int createdCount = service.createDueRuns(50);

        assertThat(createdCount).isEqualTo(1);
        verify(runCreator).create(
                condition,
                COMPANY_ID,
                CollectionRunTriggerType.SCHEDULED,
                null,
                LocalDateTime.of(2026, 8, 11, 10, 0)
        );
        verify(scheduledConditionPort).recordScheduledRun(
                CONDITION_ID,
                LocalDateTime.of(2026, 8, 11, 9, 0),
                LocalDateTime.of(2026, 8, 12, 9, 0),
                LocalDateTime.of(2026, 8, 11, 10, 0)
        );
    }

    @Test
    @DisplayName("진행 중인 Run이 있으면 자동 Run을 만들지 않고 다음 회차로 넘긴다")
    void skipsConditionWithActiveRun() {
        CollectionCondition condition = dueCondition();
        when(scheduledConditionPort.claimDueConditions(any(), eq(50)))
                .thenReturn(List.of(condition));
        when(runRepository.existsActiveByConditionId(CONDITION_ID))
                .thenReturn(true);

        int createdCount = service.createDueRuns(50);

        assertThat(createdCount).isZero();
        assertThat(condition.getLastScheduledAt()).isNull();
        verify(runCreator, never()).create(any(), any(), any(), any(), any());
        verify(scheduledConditionPort).advanceSchedule(
                CONDITION_ID,
                LocalDateTime.of(2026, 8, 12, 9, 0),
                LocalDateTime.of(2026, 8, 11, 10, 0)
        );
    }

    private CollectionCondition dueCondition() {
        return CollectionCondition.restore(
                CONDITION_ID,
                COMPANY_ID,
                "NARA",
                "자동 수집 조건",
                List.of(BidNoticeType.SERVICE),
                new CollectionConditionFilter(
                        List.of("스마트시티"),
                        List.of(),
                        List.of(),
                        null,
                        null,
                        true,
                        null
                ),
                true,
                true,
                CollectionScheduleType.DAILY,
                LocalTime.of(9, 0),
                "Asia/Seoul",
                LocalDateTime.of(2026, 8, 11, 9, 0),
                null,
                null,
                null,
                "EMP001",
                LocalDateTime.of(2026, 8, 10, 9, 0),
                null,
                null
        );
    }
}
