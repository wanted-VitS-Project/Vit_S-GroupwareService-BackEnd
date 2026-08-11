package com.group3.vitamins.bidding.collectionrun.application.service;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;
import com.group3.vitamins.bidding.collectionrun.application.model.ClaimedCollectionRun;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNoticePage;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNoticePayload;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunFailureType;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunJob;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunJobResult;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTask;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTaskSummary;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectedBidNoticeStorePort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunStatePort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunTaskPort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionSourceCollectorPort;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunConditionSnapshot;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTaskStatus;
import com.group3.vitamins.bidding.bidnotice.domain.event.BidNoticeListChangedEvent;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionRunJobHandlerService")
class CollectionRunJobHandlerServiceTest {

    private static final Long RUN_ID = 10L;
    private static final Long CONDITION_ID = 20L;
    private static final Long COMPANY_ID = 30L;
    private static final Long TASK_ID = 40L;
    private static final String ATTEMPT_ID = "attempt-1";

    private static final CollectionRequestCombination TARGET =
            new CollectionRequestCombination(
                    BidNoticeType.SERVICE,
                    "스마트시티",
                    "11",
                    "6202",
                    1
            );

    @Mock
    private CollectionRunStatePort runStatePort;

    @Mock
    private CollectionRunTaskPort taskPort;

    @Mock
    private CollectionRunTaskFailureService taskFailureService;

    @Mock
    private CollectionSourceCollectorPort collector;

    @Mock
    private CollectedBidNoticeStorePort noticeStorePort;

    @Mock
    private DomainEventPublisher eventPublisher;

    private CollectionRunJobHandlerService service;
    private CollectionRunConditionSnapshot snapshot;
    private CollectionRunJob job;
    private CollectionRunTask task;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-10T09:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        snapshot = snapshot();
        job = new CollectionRunJob(
                RUN_ID, CONDITION_ID, COMPANY_ID, ATTEMPT_ID, 0, null
        );
        task = new CollectionRunTask(
                TASK_ID,
                RUN_ID,
                TARGET,
                CollectionRunTaskStatus.PROCESSING,
                ATTEMPT_ID,
                0,
                LocalDateTime.of(2026, 8, 10, 18, 5)
        );

        lenient().when(collector.supportedSourceCode()).thenReturn("NARA");
        service = new CollectionRunJobHandlerService(
                runStatePort,
                taskPort,
                taskFailureService,
                List.of(collector),
                noticeStorePort,
                eventPublisher,
                clock
        );
    }

    @Test
    @DisplayName("실행 점유에 실패하면 중복 처리하지 않고 성공으로 종료한다")
    void returnsSuccessWhenRunIsAlreadyClaimed() {
        when(runStatePort.claim(any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(Optional.empty());

        CollectionRunJobResult result = service.handle(job);

        assertThat(result.outcome()).isEqualTo(CollectionRunJobResult.Outcome.SUCCESS);
        verify(taskPort, never()).findNextProcessableTask(any(), any());
        verify(collector, never()).collect(any(), any(), anyInt());
    }

    @Test
    @DisplayName("공고 한 페이지를 저장하고 Task와 Run을 완료한다")
    void storesNoticesAndCompletesTaskAndRun() {
        CollectedBidNoticePayload payload = mock(CollectedBidNoticePayload.class);
        prepareClaimedRunAndTask();
        when(collector.collect(snapshot, TARGET, 100))
                .thenReturn(page(List.of(payload), false));
        when(noticeStorePort.saveAll(eq(COMPANY_ID), eq("NARA"), eq(RUN_ID), anyList(), any()))
                .thenReturn(new CollectedBidNoticeStorePort.StoreResult(1, 0, 0));
        when(taskPort.complete(
                eq(TASK_ID), eq(ATTEMPT_ID), eq(1), eq(1), eq(0), eq(0), any()
        )).thenReturn(true);
        when(taskPort.summarize(RUN_ID)).thenReturn(completedSummary());
        when(runStatePort.complete(
                eq(RUN_ID), eq(CONDITION_ID), eq(ATTEMPT_ID), eq(CollectionRunStatus.COMPLETED),
                eq(1), eq(1), eq(0), eq(0), any()
        )).thenReturn(true);

        CollectionRunJobResult result = service.handle(job);

        assertThat(result.outcome()).isEqualTo(CollectionRunJobResult.Outcome.SUCCESS);
        verify(noticeStorePort).saveAll(eq(COMPANY_ID), eq("NARA"), eq(RUN_ID), anyList(), any());
        verify(runStatePort).complete(
                eq(RUN_ID), eq(CONDITION_ID), eq(ATTEMPT_ID), eq(CollectionRunStatus.COMPLETED),
                eq(1), eq(1), eq(0), eq(0), any()
        );
        verify(eventPublisher).publish(new BidNoticeListChangedEvent(COMPANY_ID));
    }

    @Test
    @DisplayName("다음 페이지가 있으면 같은 조건의 다음 페이지 Task를 생성한다")
    void createsNextPageTask() {
        prepareClaimedRunAndTask();
        when(collector.collect(snapshot, TARGET, 100))
                .thenReturn(page(List.of(), true));
        when(noticeStorePort.saveAll(eq(COMPANY_ID), eq("NARA"), eq(RUN_ID), anyList(), any()))
                .thenReturn(new CollectedBidNoticeStorePort.StoreResult(0, 0, 0));
        when(taskPort.complete(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), any()))
                .thenReturn(true);
        when(taskPort.summarize(RUN_ID)).thenReturn(completedSummary());

        service.handle(job);

        verify(taskPort).createTasks(
                RUN_ID,
                List.of(new CollectionRequestCombination(
                        BidNoticeType.SERVICE,
                        "스마트시티",
                        "11",
                        "6202",
                        2
                ))
        );
    }

    @Test
    @DisplayName("재시도 가능한 실패는 Task와 Run을 대기 상태로 되돌린다")
    void preparesRetryForRetryableFailure() {
        prepareClaimedRunAndTaskOnce();
        when(collector.collect(snapshot, TARGET, 100))
                .thenReturn(failedPage(true));

        CollectionRunJobResult result = service.handle(job);

        assertThat(result.outcome())
                .isEqualTo(CollectionRunJobResult.Outcome.RETRYABLE_FAILURE);
        assertThat(result.failureType())
                .isEqualTo(CollectionRunFailureType.CONNECTION_FAILURE);
        verify(taskPort).prepareRetry(
                eq(TASK_ID), eq(ATTEMPT_ID),
                eq("CONNECTION_FAILURE"), eq("CONNECTION_FAILURE"), any()
        );
        verify(runStatePort).prepareRetry(
                eq(RUN_ID), eq(ATTEMPT_ID),
                eq("CONNECTION_FAILURE"), eq("CONNECTION_FAILURE"), any()
        );
    }

    @Test
    @DisplayName("영구 실패한 Task만 남으면 실행을 실패로 종료한다")
    void failsRunWhenAllTasksFailed() {
        prepareClaimedRunAndTask();
        when(collector.collect(snapshot, TARGET, 100))
                .thenReturn(failedPage(false));
        when(taskFailureService.recordPermanentFailure(any(), any(), any(), any()))
                .thenReturn(true);
        when(taskPort.summarize(RUN_ID)).thenReturn(new CollectionRunTaskSummary(
                1, 0, 0, 0, 1,
                0, 0, 0, 0
        ));

        CollectionRunJobResult result = service.handle(job);

        assertThat(result.outcome()).isEqualTo(CollectionRunJobResult.Outcome.SUCCESS);
        verify(taskFailureService).recordPermanentFailure(
                argThat(failure -> failure.runId().equals(RUN_ID)
                        && failure.taskId().equals(TASK_ID)
                        && failure.companyId().equals(COMPANY_ID)
                        && failure.failureType() == CollectionRunFailureType.CONNECTION_FAILURE
                        && failure.target().equals(TARGET)),
                eq("CONNECTION_FAILURE"),
                eq("CONNECTION_FAILURE"),
                any()
        );
        verify(runStatePort).fail(
                eq(RUN_ID), eq(ATTEMPT_ID),
                eq("UNKNOWN_PROCESSING_ERROR"),
                eq("all_collection_tasks_failed"), any()
        );
    }

    @Test
    @DisplayName("영구 실패 전이가 거부되면 실행을 재시도 상태로 되돌린다")
    void retriesRunWhenPermanentFailureTransitionIsRejected() {
        prepareClaimedRunAndTaskOnce();
        when(collector.collect(snapshot, TARGET, 100)).thenReturn(failedPage(false));
        when(taskFailureService.recordPermanentFailure(any(), any(), any(), any()))
                .thenReturn(false);

        CollectionRunJobResult result = service.handle(job);

        assertThat(result.outcome()).isEqualTo(CollectionRunJobResult.Outcome.RETRYABLE_FAILURE);
        assertThat(result.failureType())
                .isEqualTo(CollectionRunFailureType.UNKNOWN_PROCESSING_ERROR);
        verify(runStatePort).prepareRetry(
                eq(RUN_ID), eq(ATTEMPT_ID), eq("CONNECTION_FAILURE"),
                eq("task_failure_transition_rejected"), any()
        );
    }

    private void prepareClaimedRunAndTask() {
        when(runStatePort.claim(any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(Optional.of(new ClaimedCollectionRun(RUN_ID, snapshot)));
        when(taskPort.findNextProcessableTask(eq(RUN_ID), any()))
                .thenReturn(Optional.of(task))
                .thenReturn(Optional.empty());
        when(taskPort.claim(
                eq(RUN_ID), eq(TARGET), eq(ATTEMPT_ID), eq(0), any(), any()
        )).thenReturn(Optional.of(task));
    }

    private void prepareClaimedRunAndTaskOnce() {
        when(runStatePort.claim(any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(Optional.of(new ClaimedCollectionRun(RUN_ID, snapshot)));
        when(taskPort.findNextProcessableTask(eq(RUN_ID), any()))
                .thenReturn(Optional.of(task));
        when(taskPort.claim(
                eq(RUN_ID), eq(TARGET), eq(ATTEMPT_ID), eq(0), any(), any()
        )).thenReturn(Optional.of(task));
    }

    private CollectedBidNoticePage page(
            List<CollectedBidNoticePayload> notices,
            boolean hasNext
    ) {
        return new CollectedBidNoticePage(notices, List.of(), 1, notices.size(), hasNext);
    }

    private CollectedBidNoticePage failedPage(boolean retryable) {
        return new CollectedBidNoticePage(
                List.of(),
                List.of(new CollectedBidNoticePage.CollectionFailure(
                        BidNoticeType.SERVICE,
                        "스마트시티",
                        "11",
                        "6202",
                        1,
                        CollectionRunFailureType.CONNECTION_FAILURE,
                        retryable
                )),
                1,
                0,
                false
        );
    }

    private CollectionRunTaskSummary completedSummary() {
        return new CollectionRunTaskSummary(
                1, 0, 0, 1, 0,
                1, 1, 0, 0
        );
    }

    private CollectionRunConditionSnapshot snapshot() {
        return new CollectionRunConditionSnapshot(
                "NARA",
                "수도권 스마트시티",
                List.of(BidNoticeType.SERVICE),
                new CollectionConditionFilter(
                        List.of("스마트시티"),
                        List.of("11"),
                        List.of("6202"),
                        null,
                        null,
                        true,
                        InternationalBidType.DOMESTIC
                ),
                LocalDateTime.of(2026, 8, 9, 18, 0),
                LocalDateTime.of(2026, 8, 10, 18, 0)
        );
    }
}
