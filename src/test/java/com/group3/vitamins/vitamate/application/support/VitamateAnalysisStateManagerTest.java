package com.group3.vitamins.vitamate.application.support;

import com.group3.vitamins.vitamate.application.port.VitamateAnalysisStorePort;
import com.group3.vitamins.vitamate.application.result.StartVitamateAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("VitamateAnalysisStateManager 상태 전이")
class VitamateAnalysisStateManagerTest {

    private static final Long ANALYSIS_ID = 1L;
    private static final String ATTEMPT_ID = "attempt-1";

    private VitamateAnalysisStorePort analysisStore;
    private VitamateAnalysisStateManager stateManager;

    @BeforeEach
    void setUp() {
        analysisStore = mock(VitamateAnalysisStorePort.class);
        stateManager = new VitamateAnalysisStateManager(analysisStore);
    }

    @Nested
    @DisplayName("PROCESSING 선점")
    class StartProcessing {

        @Test
        @DisplayName("PENDING 분석을 PROCESSING으로 선점하면 attempt와 lease 정보를 반환한다")
        void startsPendingAnalysis() {
            when(analysisStore.markProcessing(
                    eq(ANALYSIS_ID),
                    anyString(),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            )).thenReturn(true);

            Optional<StartVitamateAnalysisResult> result = stateManager.startProcessing(ANALYSIS_ID);

            assertThat(result).isPresent();
            StartVitamateAnalysisResult started = result.get();
            assertThat(started.analysisId()).isEqualTo(ANALYSIS_ID);
            assertThat(started.attemptId()).isNotBlank();
            assertThat(Duration.between(started.startedAt(), started.leaseExpiresAt()))
                    .isEqualTo(Duration.ofMinutes(10));
            verify(analysisStore).markProcessing(
                    ANALYSIS_ID,
                    started.attemptId(),
                    started.startedAt(),
                    started.leaseExpiresAt()
            );
        }

        @Test
        @DisplayName("이미 선점된 분석이면 빈 결과를 반환한다")
        void returnsEmptyWhenAnalysisAlreadyClaimed() {
            when(analysisStore.markProcessing(
                    eq(ANALYSIS_ID),
                    anyString(),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            )).thenReturn(false);

            Optional<StartVitamateAnalysisResult> result = stateManager.startProcessing(ANALYSIS_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("COMPLETED 마감")
    class CompleteProcessing {

        @Test
        @DisplayName("유효한 워커 시도이면 분석 결과를 COMPLETED로 저장한다")
        void completesProcessingAnalysis() {
            when(analysisStore.markCompleted(
                    eq(ANALYSIS_ID),
                    eq(ATTEMPT_ID),
                    eq("analysis result"),
                    any(LocalDateTime.class)
            )).thenReturn(true);

            boolean completed = stateManager.completeProcessing(ANALYSIS_ID, ATTEMPT_ID, "analysis result");

            assertThat(completed).isTrue();
        }

        @Test
        @DisplayName("워커 시도가 유효하지 않으면 COMPLETED 저장에 실패한다")
        void returnsFalseWhenCompleteConditionDoesNotMatch() {
            when(analysisStore.markCompleted(
                    eq(ANALYSIS_ID),
                    eq(ATTEMPT_ID),
                    eq("analysis result"),
                    any(LocalDateTime.class)
            )).thenReturn(false);

            boolean completed = stateManager.completeProcessing(ANALYSIS_ID, ATTEMPT_ID, "analysis result");

            assertThat(completed).isFalse();
        }
    }

    @Nested
    @DisplayName("FAILED 마감")
    class FailProcessing {

        @Test
        @DisplayName("PROCESSING 분석을 현재 워커 시도로 FAILED 처리한다")
        void failsProcessingAnalysis() {
            when(analysisStore.markFailedFromProcessing(
                    eq(ANALYSIS_ID),
                    eq(ATTEMPT_ID),
                    eq("python error"),
                    any(LocalDateTime.class)
            )).thenReturn(true);

            boolean failed = stateManager.failProcessing(ANALYSIS_ID, ATTEMPT_ID, "python error");

            assertThat(failed).isTrue();
        }

        @Test
        @DisplayName("PENDING 분석을 FAILED로 마감한다")
        void failsPendingAnalysis() {
            when(analysisStore.markFailedFromPending(
                    eq(ANALYSIS_ID),
                    eq("validation error"),
                    any(LocalDateTime.class)
            )).thenReturn(true);

            boolean failed = stateManager.failPending(ANALYSIS_ID, "validation error");

            assertThat(failed).isTrue();
        }
    }

    @Nested
    @DisplayName("입력값 검증")
    class ValidateInput {

        @Test
        @DisplayName("분석 ID가 없으면 Store를 호출하지 않는다")
        void rejectsMissingAnalysisId() {
            assertThatThrownBy(() -> stateManager.startProcessing(null))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(analysisStore, never()).markProcessing(any(), anyString(), any(), any());
        }

        @Test
        @DisplayName("attemptId가 비어 있으면 완료 저장을 호출하지 않는다")
        void rejectsBlankAttemptId() {
            assertThatThrownBy(() -> stateManager.completeProcessing(ANALYSIS_ID, " ", "result"))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(analysisStore, never()).markCompleted(any(), anyString(), anyString(), any());
        }
    }
}
