package com.group3.vitamins.vitamate.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.application.command.HandleVitamateAnalysisCallbackCommand;
import com.group3.vitamins.vitamate.application.port.VitamateAnalysisStorePort;
import com.group3.vitamins.vitamate.application.result.VitamateAnalysisCallbackResult;
import com.group3.vitamins.vitamate.application.support.VitamateAnalysisStateManager;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("VitamateAnalysisCallbackService")
class VitamateAnalysisCallbackServiceTest {

    private static final Long ANALYSIS_ID = 1L;
    private static final String ATTEMPT_ID = "attempt-1";

    private VitamateAnalysisStateManager stateManager;
    private VitamateAnalysisStorePort analysisStore;
    private VitamateAnalysisCallbackService callbackService;

    @BeforeEach
    void setUp() {
        stateManager = mock(VitamateAnalysisStateManager.class);
        analysisStore = mock(VitamateAnalysisStorePort.class);
        callbackService = new VitamateAnalysisCallbackService(stateManager, analysisStore);
    }

    @Nested
    @DisplayName("COMPLETED callback")
    class CompletedCallback {

        @Test
        @DisplayName("citation 범위가 유효하면 결과와 citation을 저장한다")
        void savesCompletedResultAndCitations() {
            when(analysisStore.existsAllCitationTargets(eq(ANALYSIS_ID), anyList()))
                    .thenReturn(true);
            when(stateManager.completeProcessing(ANALYSIS_ID, ATTEMPT_ID, "analysis result"))
                    .thenReturn(true);

            VitamateAnalysisCallbackResult result = callbackService.handle(completedCommand());

            assertThat(result.accepted()).isTrue();
            assertThat(result.analysisStatus()).isEqualTo("COMPLETED");
            verify(stateManager).completeProcessing(ANALYSIS_ID, ATTEMPT_ID, "analysis result");
            verify(analysisStore).saveAnalysisCitations(eq(ANALYSIS_ID), anyList());
        }

        @Test
        @DisplayName("citation이 선택 문서 범위를 벗어나면 FAILED로 마감한다")
        void failsWhenCitationTargetsAreInvalid() {
            when(analysisStore.existsAllCitationTargets(eq(ANALYSIS_ID), anyList()))
                    .thenReturn(false);
            when(stateManager.failProcessing(
                    ANALYSIS_ID,
                    ATTEMPT_ID,
                    "분석 근거가 선택 문서 범위를 벗어났습니다."
            )).thenReturn(true);

            VitamateAnalysisCallbackResult result = callbackService.handle(completedCommand());

            assertThat(result.accepted()).isTrue();
            assertThat(result.analysisStatus()).isEqualTo("FAILED");
            verify(stateManager, never()).completeProcessing(eq(ANALYSIS_ID), eq(ATTEMPT_ID), eq("analysis result"));
            verify(analysisStore, never()).saveAnalysisCitations(eq(ANALYSIS_ID), anyList());
        }

        @Test
        @DisplayName("현재 worker 시도가 아니면 callback을 무시한다")
        void ignoresWhenAttemptDoesNotMatch() {
            when(analysisStore.existsAllCitationTargets(eq(ANALYSIS_ID), anyList()))
                    .thenReturn(true);
            when(stateManager.completeProcessing(ANALYSIS_ID, ATTEMPT_ID, "analysis result"))
                    .thenReturn(false);
            when(analysisStore.findAnalysisStatus(ANALYSIS_ID))
                    .thenReturn(Optional.of("FAILED"));

            VitamateAnalysisCallbackResult result = callbackService.handle(completedCommand());

            assertThat(result.accepted()).isFalse();
            assertThat(result.analysisStatus()).isEqualTo("FAILED");
            assertThat(result.reason()).isEqualTo("attempt_mismatch_or_already_finished");
            verify(analysisStore, never()).saveAnalysisCitations(eq(ANALYSIS_ID), anyList());
        }
    }

    @Nested
    @DisplayName("FAILED callback")
    class FailedCallback {

        @Test
        @DisplayName("실패 사유를 저장하고 FAILED로 마감한다")
        void savesFailedResult() {
            when(stateManager.failProcessing(ANALYSIS_ID, ATTEMPT_ID, "python error"))
                    .thenReturn(true);

            VitamateAnalysisCallbackResult result = callbackService.handle(failedCommand());

            assertThat(result.accepted()).isTrue();
            assertThat(result.analysisStatus()).isEqualTo("FAILED");
            verify(stateManager).failProcessing(ANALYSIS_ID, ATTEMPT_ID, "python error");
            verify(analysisStore, never()).saveAnalysisCitations(eq(ANALYSIS_ID), anyList());
        }
    }

    @Nested
    @DisplayName("입력값 검증")
    class ValidateInput {

        @Test
        @DisplayName("지원하지 않는 상태는 처리하지 않는다")
        void rejectsUnsupportedStatus() {
            HandleVitamateAnalysisCallbackCommand command = new HandleVitamateAnalysisCallbackCommand(
                    ANALYSIS_ID,
                    ATTEMPT_ID,
                    "PENDING",
                    "analysis result",
                    List.of(),
                    null
            );

            assertThatThrownBy(() -> callbackService.handle(command))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verifyNoInteractions(stateManager, analysisStore);
        }

        @Test
        @DisplayName("citation 순번이 중복되면 처리하지 않는다")
        void rejectsDuplicateCitationRankOrder() {
            HandleVitamateAnalysisCallbackCommand command = new HandleVitamateAnalysisCallbackCommand(
                    ANALYSIS_ID,
                    ATTEMPT_ID,
                    "COMPLETED",
                    "analysis result",
                    List.of(
                            new HandleVitamateAnalysisCallbackCommand.Citation(
                                    3001L,
                                    101L,
                                    1,
                                    BigDecimal.valueOf(0.123456),
                                    "첫 번째 근거"
                            ),
                            new HandleVitamateAnalysisCallbackCommand.Citation(
                                    3002L,
                                    101L,
                                    1,
                                    BigDecimal.valueOf(0.234567),
                                    "두 번째 근거"
                            )
                    ),
                    null
            );

            assertThatThrownBy(() -> callbackService.handle(command))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verifyNoInteractions(stateManager, analysisStore);
        }
    }

    // 성공 callback 테스트에 사용할 command를 만든다.
    private HandleVitamateAnalysisCallbackCommand completedCommand() {
        return new HandleVitamateAnalysisCallbackCommand(
                ANALYSIS_ID,
                ATTEMPT_ID,
                "COMPLETED",
                "analysis result",
                List.of(new HandleVitamateAnalysisCallbackCommand.Citation(
                        3001L,
                        101L,
                        1,
                        BigDecimal.valueOf(0.123456),
                        "핵심 요구사항 근거"
                )),
                null
        );
    }

    // 실패 callback 테스트에 사용할 command를 만든다.
    private HandleVitamateAnalysisCallbackCommand failedCommand() {
        return new HandleVitamateAnalysisCallbackCommand(
                ANALYSIS_ID,
                ATTEMPT_ID,
                "FAILED",
                null,
                List.of(),
                "python error"
        );
    }
}
