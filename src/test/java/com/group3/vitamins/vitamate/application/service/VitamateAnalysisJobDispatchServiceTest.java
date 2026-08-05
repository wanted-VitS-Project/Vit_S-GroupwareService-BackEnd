package com.group3.vitamins.vitamate.application.service;

import com.group3.vitamins.vitamate.application.command.DispatchVitamateAnalysisJobCommand;
import com.group3.vitamins.vitamate.application.port.VitamateAnalysisJobPublisherPort;
import com.group3.vitamins.vitamate.application.result.StartVitamateAnalysisResult;
import com.group3.vitamins.vitamate.application.support.VitamateAnalysisStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("VitamateAnalysisJobDispatchService")
class VitamateAnalysisJobDispatchServiceTest {

    private static final Long ANALYSIS_ID = 1L;
    private static final String ATTEMPT_ID = "attempt-1";
    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 8, 5, 10, 0);
    private static final LocalDateTime LEASE_EXPIRES_AT = STARTED_AT.plusMinutes(10);

    private VitamateAnalysisStateManager stateManager;
    private VitamateAnalysisJobPublisherPort jobPublisherPort;
    private VitamateAnalysisJobDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        stateManager = mock(VitamateAnalysisStateManager.class);
        jobPublisherPort = mock(VitamateAnalysisJobPublisherPort.class);
        dispatchService = new VitamateAnalysisJobDispatchService(stateManager, jobPublisherPort);
    }

    @Nested
    @DisplayName("작업 발행")
    class DispatchJob {

        @Test
        @DisplayName("PENDING 분석을 선점하면 Redis 작업 메시지를 발행한다")
        void publishesJobWhenAnalysisIsClaimed() {
            when(stateManager.startProcessing(ANALYSIS_ID))
                    .thenReturn(Optional.of(startedResult()));

            LocalDateTime beforePublish = LocalDateTime.now();
            dispatchService.handle(command());
            LocalDateTime afterPublish = LocalDateTime.now();

            ArgumentCaptor<VitamateAnalysisJobPublisherPort.AnalysisJob> captor =
                    ArgumentCaptor.forClass(VitamateAnalysisJobPublisherPort.AnalysisJob.class);
            verify(jobPublisherPort).publish(captor.capture());

            VitamateAnalysisJobPublisherPort.AnalysisJob job = captor.getValue();
            assertThat(job.analysisId()).isEqualTo(ANALYSIS_ID);
            assertThat(job.attemptId()).isEqualTo(ATTEMPT_ID);
            assertThat(job.retryCount()).isZero();
            assertThat(job.createdAt())
                    .isAfterOrEqualTo(beforePublish)
                    .isBeforeOrEqualTo(afterPublish);
        }

        @Test
        @DisplayName("이미 선점된 분석이면 큐 발행을 건너뛴다")
        void skipsWhenAnalysisIsAlreadyClaimed() {
            when(stateManager.startProcessing(ANALYSIS_ID))
                    .thenReturn(Optional.empty());

            dispatchService.handle(command());

            verify(jobPublisherPort, never()).publish(any());
        }

        @Test
        @DisplayName("Redis 발행에 실패하면 PROCESSING 분석을 FAILED로 마감한다")
        void failsProcessingWhenPublishThrowsException() {
            when(stateManager.startProcessing(ANALYSIS_ID))
                    .thenReturn(Optional.of(startedResult()));
            doThrow(new RuntimeException("redis down"))
                    .when(jobPublisherPort)
                    .publish(any());

            dispatchService.handle(command());

            verify(stateManager).failProcessing(
                    ANALYSIS_ID,
                    ATTEMPT_ID,
                    "분석 작업 큐 발행에 실패했습니다."
            );
        }
    }

    @Nested
    @DisplayName("입력값 검증")
    class ValidateInput {

        @Test
        @DisplayName("분석 ID가 없으면 상태 선점과 큐 발행을 하지 않는다")
        void rejectsMissingAnalysisId() {
            assertThatThrownBy(() -> dispatchService.handle(new DispatchVitamateAnalysisJobCommand(null)))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(stateManager, jobPublisherPort);
        }

        @Test
        @DisplayName("분석 ID가 0 이하면 상태 선점과 큐 발행을 하지 않는다")
        void rejectsNonPositiveAnalysisId() {
            assertThatThrownBy(() -> dispatchService.handle(new DispatchVitamateAnalysisJobCommand(0L)))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(stateManager, jobPublisherPort);
        }
    }

    // 큐 발행 테스트에 사용할 command를 만든다.
    private DispatchVitamateAnalysisJobCommand command() {
        return new DispatchVitamateAnalysisJobCommand(ANALYSIS_ID);
    }

    // 상태 선점 성공 결과를 만든다.
    private StartVitamateAnalysisResult startedResult() {
        return new StartVitamateAnalysisResult(
                ANALYSIS_ID,
                ATTEMPT_ID,
                STARTED_AT,
                LEASE_EXPIRES_AT
        );
    }
}
