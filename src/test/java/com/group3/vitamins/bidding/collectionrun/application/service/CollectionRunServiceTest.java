package com.group3.vitamins.bidding.collectionrun.application.service;

import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import com.group3.vitamins.bidding.collectionrun.application.command.StartCollectionRunCommand;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunOutbox;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunConditionPort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunOutboxStorePort;
import com.group3.vitamins.bidding.collectionrun.application.query.GetCollectionRunQuery;
import com.group3.vitamins.bidding.collectionrun.application.result.CollectionRunResult;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRun;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunConditionSnapshot;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTriggerType;
import com.group3.vitamins.bidding.collectionrun.domain.repository.CollectionRunRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CollectionRunService 수집 실행 관리")
class CollectionRunServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long CONDITION_ID = 20L;
    private static final Long RUN_ID = 30L;
    private static final String USER_ID = "EMP001";

    private CollectionRunConditionPort conditionPort;
    private CollectionRunRepository runRepository;
    private CollectionRunOutboxStorePort outboxStorePort;
    private CurrentCompanyIdProvider companyIdProvider;
    private Clock clock;
    private CollectionRunService service;

    @BeforeEach
    void setUp() {
        conditionPort = mock(CollectionRunConditionPort.class);
        runRepository = mock(CollectionRunRepository.class);
        outboxStorePort = mock(CollectionRunOutboxStorePort.class);
        companyIdProvider = mock(CurrentCompanyIdProvider.class);
        clock = Clock.fixed(
                Instant.parse("2026-08-10T06:00:00Z"),
                ZoneOffset.UTC
        );

        when(companyIdProvider.currentCompanyId())
                .thenReturn(COMPANY_ID);

        service = new CollectionRunService(
                conditionPort,
                runRepository,
                outboxStorePort,
                companyIdProvider,
                clock
        );
    }

    @Test
    @DisplayName("현재 회사의 활성 조건으로 PENDING 실행을 생성한다")
    void startsPendingRun() {
        CollectionCondition condition = activeCondition();

        when(conditionPort.findOwnedConditionForUpdate(
                CONDITION_ID,
                COMPANY_ID
        )).thenReturn(Optional.of(condition));

        when(runRepository.existsActiveByConditionId(CONDITION_ID))
                .thenReturn(false);

        when(runRepository.save(any(CollectionRun.class)))
                .thenAnswer(invocation ->
                        persistedRun(invocation.getArgument(0))
                );

        CollectionRunResult result = service.start(
                new StartCollectionRunCommand(CONDITION_ID, USER_ID)
        );

        assertThat(result.runId()).isEqualTo(RUN_ID);
        assertThat(result.conditionId()).isEqualTo(CONDITION_ID);
        assertThat(result.runStatus())
                .isEqualTo(CollectionRunStatus.PENDING);

        ArgumentCaptor<CollectionRun> captor =
                ArgumentCaptor.forClass(CollectionRun.class);

        verify(runRepository).save(captor.capture());

        CollectionRun saved = captor.getValue();
        assertThat(saved.conditionId()).isEqualTo(CONDITION_ID);
        assertThat(saved.requestedBy()).isEqualTo(USER_ID);
        assertThat(saved.runStatus())
                .isEqualTo(CollectionRunStatus.PENDING);

        ArgumentCaptor<CollectionRunOutbox.Pending> outboxCaptor =
                ArgumentCaptor.forClass(CollectionRunOutbox.Pending.class);

        verify(outboxStorePort).savePending(outboxCaptor.capture());

        CollectionRunOutbox.Pending outbox = outboxCaptor.getValue();
        assertThat(outbox.runId()).isEqualTo(RUN_ID);
        assertThat(outbox.conditionId()).isEqualTo(CONDITION_ID);
        assertThat(outbox.companyId()).isEqualTo(COMPANY_ID);
        assertThat(outbox.eventType())
                .isEqualTo("COLLECTION_RUN_REQUESTED");
        assertThat(outbox.retryCount()).isZero();
        assertThat(outbox.eventId()).isNotBlank();
        assertThat(outbox.attemptId()).isNotBlank();
        assertThat(outbox.createdAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 10, 6, 0));
    }

    @Test
    @DisplayName("다른 회사이거나 존재하지 않는 조건은 실행할 수 없다")
    void rejectsMissingOwnedCondition() {
        when(conditionPort.findOwnedConditionForUpdate(
                CONDITION_ID,
                COMPANY_ID
        )).thenReturn(Optional.empty());

        assertError(
                () -> service.start(
                        new StartCollectionRunCommand(
                                CONDITION_ID,
                                USER_ID
                        )
                ),
                BiddingErrorCode.BIDDING_COLLECTION_CONDITION_NOT_FOUND
        );

        verify(runRepository, never())
                .existsActiveByConditionId(any());
        verify(runRepository, never()).save(any());
    }

    @Test
    @DisplayName("비활성 수집 조건은 실행할 수 없다")
    void rejectsInactiveCondition() {
        CollectionCondition condition = mock(CollectionCondition.class);

        when(condition.getConditionId()).thenReturn(CONDITION_ID);
        when(condition.isActive()).thenReturn(false);

        when(conditionPort.findOwnedConditionForUpdate(
                CONDITION_ID,
                COMPANY_ID
        )).thenReturn(Optional.of(condition));

        assertError(
                () -> service.start(
                        new StartCollectionRunCommand(
                                CONDITION_ID,
                                USER_ID
                        )
                ),
                BiddingErrorCode.BIDDING_INACTIVE_COLLECTION_CONDITION
        );

        verify(runRepository, never())
                .existsActiveByConditionId(any());
        verify(runRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 조건에 진행 중인 실행이 있으면 중복 실행을 차단한다")
    void rejectsDuplicateActiveRun() {
        CollectionCondition condition = activeCondition();

        when(conditionPort.findOwnedConditionForUpdate(
                CONDITION_ID,
                COMPANY_ID
        )).thenReturn(Optional.of(condition));

        when(runRepository.existsActiveByConditionId(CONDITION_ID))
                .thenReturn(true);

        assertError(
                () -> service.start(
                        new StartCollectionRunCommand(
                                CONDITION_ID,
                                USER_ID
                        )
                ),
                BiddingErrorCode.BIDDING_COLLECTION_RUN_ALREADY_PROCESSING
        );

        verify(runRepository, never()).save(any());
    }

    @Test
    @DisplayName("현재 회사가 소유한 수집 실행 결과를 조회한다")
    void getsOwnedRun() {
        CollectionRun run = completedRun();

        when(runRepository.findByIdAndCompanyId(
                RUN_ID,
                COMPANY_ID
        )).thenReturn(Optional.of(run));

        CollectionRunResult result = service.get(
                new GetCollectionRunQuery(RUN_ID)
        );

        assertThat(result.runId()).isEqualTo(RUN_ID);
        assertThat(result.runStatus())
                .isEqualTo(CollectionRunStatus.COMPLETED);
        assertThat(result.collectedCount()).isEqualTo(10);
        assertThat(result.insertedCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("현재 회사가 소유하지 않은 실행은 조회할 수 없다")
    void rejectsMissingOwnedRun() {
        when(runRepository.findByIdAndCompanyId(
                RUN_ID,
                COMPANY_ID
        )).thenReturn(Optional.empty());

        assertError(
                () -> service.get(new GetCollectionRunQuery(RUN_ID)),
                BiddingErrorCode.BIDDING_COLLECTION_RUN_NOT_FOUND
        );
    }

    @Test
    @DisplayName("잘못된 실행 생성 요청은 저장소에 접근하지 않는다")
    void rejectsInvalidStartCommand() {
        assertError(
                () -> service.start(
                        new StartCollectionRunCommand(0L, " ")
                ),
                BiddingErrorCode.BIDDING_INVALID_COLLECTION_RUN_REQUEST
        );

        verify(conditionPort, never())
                .findOwnedConditionForUpdate(any(), any());
        verify(runRepository, never()).save(any());
    }

    @Test
    @DisplayName("null 조회 요청은 저장소에 접근하지 않는다")
    void rejectsNullGetQuery() {
        assertError(
                () -> service.get(null),
                BiddingErrorCode.BIDDING_INVALID_COLLECTION_RUN_REQUEST
        );

        verify(runRepository, never())
                .findByIdAndCompanyId(any(), any());
    }

    // 실행 가능한 활성 수집 조건을 만듭니다.
    private CollectionCondition activeCondition() {
        CollectionCondition condition = mock(CollectionCondition.class);

        when(condition.getConditionId()).thenReturn(CONDITION_ID);
        when(condition.getSourceCode()).thenReturn("NARA");
        when(condition.getConditionName()).thenReturn("테스트 수집 조건");
        when(condition.getNoticeTypes()).thenReturn(List.of(BidNoticeType.SERVICE));
        when(condition.getFilters()).thenReturn(testFilter());
        when(condition.isActive()).thenReturn(true);

        return condition;
    }

    // 저장소가 ID를 발급한 실행 결과를 만듭니다.
    private CollectionRun persistedRun(CollectionRun run) {
        return new CollectionRun(
                RUN_ID,
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
                run.updatedAt()
        );
    }

    // 완료된 수집 실행 조회 결과를 만듭니다.
    private CollectionRun completedRun() {
        LocalDateTime now = LocalDateTime.now();

        return new CollectionRun(
                RUN_ID,
                CONDITION_ID,
                testSnapshot(),
                CollectionRunTriggerType.MANUAL,
                CollectionRunStatus.COMPLETED,
                null,
                0,
                null,
                null,
                now.minusMinutes(1),
                now,
                10,
                7,
                2,
                1,
                null,
                null,
                USER_ID,
                now.minusMinutes(1),
                now
        );
    }

    // 테스트에서 사용하는 실행 시점 수집 조건 스냅샷을 만듭니다.
    private CollectionRunConditionSnapshot testSnapshot() {
        return new CollectionRunConditionSnapshot(
                "NARA",
                "테스트 수집 조건",
                List.of(BidNoticeType.SERVICE),
                testFilter()
        );
    }

    // 테스트에서 사용하는 최소 수집 필터를 만듭니다.
    private CollectionConditionFilter testFilter() {
        return new CollectionConditionFilter(
                List.of("스마트시티"),
                List.of(),
                List.of(),
                null,
                null,
                true,
                null
        );
    }

    // 기대한 입찰 오류 코드가 발생했는지 검증합니다.
    private void assertError(
            Runnable action,
            BiddingErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .satisfies(exception ->
                        assertThat(
                                ((DomainException) exception)
                                        .getErrorCode()
                        ).isEqualTo(expectedErrorCode)
                );
    }
}
