package com.group3.vitamins.vitamate.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisReaderPort;
import com.group3.vitamins.vitamate.analysis.application.query.GetVitamateAnalysisJobQuery;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisJobDetailResult;
import com.group3.vitamins.vitamate.analysis.application.service.VitamateAnalysisJobQueryService;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("VitamateAnalysisJobQueryService")
class VitamateAnalysisJobQueryServiceTest {

    private static final Long ANALYSIS_ID = 1L;
    private static final String ATTEMPT_ID = "attempt-1";

    private VitamateAnalysisReaderPort analysisReader;
    private VitamateAnalysisJobQueryService queryService;

    @BeforeEach
    void setUp() {
        analysisReader = mock(VitamateAnalysisReaderPort.class);
        queryService = new VitamateAnalysisJobQueryService(analysisReader);
    }

    @Nested
    @DisplayName("get analysis job")
    class GetAnalysisJob {

        @Test
        @DisplayName("returns processing analysis job")
        void returnsProcessingAnalysisJob() {
            VitamateAnalysisReaderPort.VitamateAnalysisJobDetail detail = analysisJobDetail();
            when(analysisReader.findProcessingAnalysisJob(ANALYSIS_ID, ATTEMPT_ID))
                    .thenReturn(Optional.of(detail));

            VitamateAnalysisJobDetailResult result = queryService.handle(query());

            assertThat(result.analysisId()).isEqualTo(ANALYSIS_ID);
            assertThat(result.attemptId()).isEqualTo(ATTEMPT_ID);
            assertThat(result.prompt()).isEqualTo("Summarize core requirements.");
            assertThat(result.searchScope().projectId()).isEqualTo(10L);
            assertThat(result.searchScope().blockId()).isEqualTo(20L);
            assertThat(result.searchScope().fileVersionIds()).containsExactly(101L);
            assertThat(result.documents())
                    .hasSize(1)
                    .first()
                    .satisfies(document -> {
                        assertThat(document.fileVersionId()).isEqualTo(101L);
                        assertThat(document.fileName()).isEqualTo("proposal.pdf");
                        assertThat(document.chunks())
                                .hasSize(1)
                                .first()
                                .satisfies(chunk -> {
                                    assertThat(chunk.documentChunkId()).isEqualTo(3001L);
                                    assertThat(chunk.chromaId()).isEqualTo("fv101-chunk-1");
                                    assertThat(chunk.pageNumber()).isEqualTo(3);
                                    assertThat(chunk.excerpt()).isEqualTo("scope and security requirements");
                                });
                    });
        }

        @Test
        @DisplayName("throws not found when job is unavailable")
        void throwsNotFoundWhenAnalysisJobIsNotAvailable() {
            when(analysisReader.findProcessingAnalysisJob(ANALYSIS_ID, ATTEMPT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryService.handle(query()))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_ANALYSIS_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("input validation")
    class ValidateInput {

        @Test
        @DisplayName("rejects missing analysis id")
        void rejectsMissingAnalysisId() {
            assertThatThrownBy(() -> queryService.handle(new GetVitamateAnalysisJobQuery(null, ATTEMPT_ID)))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verify(analysisReader, never()).findProcessingAnalysisJob(null, ATTEMPT_ID);
        }

        @Test
        @DisplayName("rejects non-positive analysis id")
        void rejectsNonPositiveAnalysisId() {
            assertThatThrownBy(() -> queryService.handle(new GetVitamateAnalysisJobQuery(0L, ATTEMPT_ID)))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verify(analysisReader, never()).findProcessingAnalysisJob(0L, ATTEMPT_ID);
        }

        @Test
        @DisplayName("rejects blank attempt id")
        void rejectsBlankAttemptId() {
            assertThatThrownBy(() -> queryService.handle(new GetVitamateAnalysisJobQuery(ANALYSIS_ID, " ")))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verify(analysisReader, never()).findProcessingAnalysisJob(ANALYSIS_ID, " ");
        }
    }

    // Build the query used by job lookup tests.
    private GetVitamateAnalysisJobQuery query() {
        return new GetVitamateAnalysisJobQuery(ANALYSIS_ID, ATTEMPT_ID);
    }

    // Build a representative job detail returned by the reader port.
    private VitamateAnalysisReaderPort.VitamateAnalysisJobDetail analysisJobDetail() {
        return new VitamateAnalysisReaderPort.VitamateAnalysisJobDetail(
                ANALYSIS_ID,
                ATTEMPT_ID,
                "Summarize core requirements.",
                new VitamateAnalysisReaderPort.JobSearchScope(
                        10L,
                        20L,
                        List.of(101L)
                ),
                List.of(new VitamateAnalysisReaderPort.JobDocument(
                        101L,
                        "proposal.pdf",
                        List.of(new VitamateAnalysisReaderPort.JobChunk(
                                3001L,
                                "fv101-chunk-1",
                                3,
                                "scope and security requirements"
                        ))
                ))
        );
    }
}
