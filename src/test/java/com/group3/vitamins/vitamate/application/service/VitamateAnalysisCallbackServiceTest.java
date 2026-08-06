package com.group3.vitamins.vitamate.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.application.command.HandleVitamateAnalysisCallbackCommand;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisStorePort;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisCallbackResult;
import com.group3.vitamins.vitamate.analysis.application.service.VitamateAnalysisCallbackService;
import com.group3.vitamins.vitamate.analysis.application.support.VitamateAnalysisStateManager;
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
    private static final String INVALID_CITATION_TARGET_MESSAGE = "Citation target is outside selected documents.";

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
        @DisplayName("saves completed result and citations")
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
        @DisplayName("marks failed when citation targets are invalid")
        void failsWhenCitationTargetsAreInvalid() {
            when(analysisStore.existsAllCitationTargets(eq(ANALYSIS_ID), anyList()))
                    .thenReturn(false);
            when(stateManager.failProcessing(ANALYSIS_ID, ATTEMPT_ID, INVALID_CITATION_TARGET_MESSAGE))
                    .thenReturn(true);

            VitamateAnalysisCallbackResult result = callbackService.handle(completedCommand());

            assertThat(result.accepted()).isTrue();
            assertThat(result.analysisStatus()).isEqualTo("FAILED");
            verify(stateManager, never()).completeProcessing(eq(ANALYSIS_ID), eq(ATTEMPT_ID), eq("analysis result"));
            verify(analysisStore, never()).saveAnalysisCitations(eq(ANALYSIS_ID), anyList());
        }

        @Test
        @DisplayName("ignores stale worker callback")
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
        @DisplayName("saves failed result")
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
    @DisplayName("input validation")
    class ValidateInput {

        @Test
        @DisplayName("rejects unsupported status")
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
        @DisplayName("rejects duplicate citation rank")
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
                                    "first citation"
                            ),
                            new HandleVitamateAnalysisCallbackCommand.Citation(
                                    3002L,
                                    101L,
                                    1,
                                    BigDecimal.valueOf(0.234567),
                                    "second citation"
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

    // Build a valid completed callback command used by happy-path tests.
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
                        "core requirement evidence"
                )),
                null
        );
    }

    // Build a valid failed callback command used by failure-state tests.
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
