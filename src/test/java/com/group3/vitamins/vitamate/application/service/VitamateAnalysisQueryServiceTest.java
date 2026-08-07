package com.group3.vitamins.vitamate.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisReaderPort;
import com.group3.vitamins.vitamate.analysis.application.query.GetVitamateAnalysisQuery;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisDetailResult;
import com.group3.vitamins.vitamate.analysis.application.service.VitamateAnalysisQueryService;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("VitamateAnalysisQueryService")
class VitamateAnalysisQueryServiceTest {

    private static final Long ANALYSIS_ID = 1L;
    private static final String USER_ID = "EMP001";

    private VitamateAnalysisReaderPort analysisReader;
    private VitamateAnalysisQueryService queryService;

    @BeforeEach
    void setUp() {
        analysisReader = mock(VitamateAnalysisReaderPort.class);
        queryService = new VitamateAnalysisQueryService(analysisReader);
    }

    @Nested
    @DisplayName("get analysis")
    class GetAnalysis {

        @Test
        @DisplayName("returns accessible analysis detail")
        void returnsAccessibleAnalysisDetail() {
            VitamateAnalysisReaderPort.VitamateAnalysisDetail detail = analysisDetail();
            when(analysisReader.findAccessibleAnalysis(ANALYSIS_ID, USER_ID))
                    .thenReturn(Optional.of(detail));

            VitamateAnalysisDetailResult result = queryService.handle(query());

            assertThat(result.analysisId()).isEqualTo(ANALYSIS_ID);
            assertThat(result.blockId()).isEqualTo(10L);
            assertThat(result.reviewType()).isEqualTo("COST_REPORT");
            assertThat(result.reviewCategoryCodes()).containsExactly("COMMON", "COST_RESULT");
            assertThat(result.additionalInstruction()).isEqualTo("금액 산식도 확인해줘.");
            assertThat(result.promptTemplateVersion()).isEqualTo("COST_REPORT_V1");
            assertThat(result.analysisStatus()).isEqualTo("COMPLETED");
            assertThat(result.result()).isEqualTo("analysis result");
            assertThat(result.errorMessage()).isNull();
            assertThat(result.documents())
                    .hasSize(1)
                    .first()
                    .satisfies(document -> {
                        assertThat(document.fileVersionId()).isEqualTo(101L);
                        assertThat(document.fileName()).isEqualTo("proposal.pdf");
                    });
            assertThat(result.citations())
                    .hasSize(1)
                    .first()
                    .satisfies(citation -> {
                        assertThat(citation.rankOrder()).isEqualTo(1);
                        assertThat(citation.fileVersionId()).isEqualTo(101L);
                        assertThat(citation.documentChunkId()).isEqualTo(3001L);
                        assertThat(citation.pageNumber()).isEqualTo(4);
                        assertThat(citation.excerpt()).isEqualTo("security requirement excerpt");
                    });
        }

        @Test
        @DisplayName("throws not found when analysis is not accessible")
        void throwsNotFoundWhenAnalysisIsNotAccessible() {
            when(analysisReader.findAccessibleAnalysis(ANALYSIS_ID, USER_ID))
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
            assertThatThrownBy(() -> queryService.handle(new GetVitamateAnalysisQuery(null, USER_ID)))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verify(analysisReader, never()).findAccessibleAnalysis(null, USER_ID);
        }

        @Test
        @DisplayName("rejects non-positive analysis id")
        void rejectsNonPositiveAnalysisId() {
            assertThatThrownBy(() -> queryService.handle(new GetVitamateAnalysisQuery(0L, USER_ID)))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verify(analysisReader, never()).findAccessibleAnalysis(0L, USER_ID);
        }

        @Test
        @DisplayName("rejects blank user id")
        void rejectsBlankUserId() {
            assertThatThrownBy(() -> queryService.handle(new GetVitamateAnalysisQuery(ANALYSIS_ID, " ")))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verify(analysisReader, never()).findAccessibleAnalysis(ANALYSIS_ID, " ");
        }
    }

    // Build the query used by analysis lookup tests.
    private GetVitamateAnalysisQuery query() {
        return new GetVitamateAnalysisQuery(ANALYSIS_ID, USER_ID);
    }

    // Build a representative analysis detail returned by the reader port.
    private VitamateAnalysisReaderPort.VitamateAnalysisDetail analysisDetail() {
        return new VitamateAnalysisReaderPort.VitamateAnalysisDetail(
                ANALYSIS_ID,
                10L,
                "COST_REPORT",
                List.of("COMMON", "COST_RESULT"),
                "금액 산식도 확인해줘.",
                "COST_REPORT_V1",
                "COMPLETED",
                "analysis result",
                null,
                LocalDateTime.of(2026, 8, 4, 14, 5),
                LocalDateTime.of(2026, 8, 4, 14, 8),
                List.of(new VitamateAnalysisReaderPort.Document(
                        101L,
                        "proposal.pdf"
                )),
                List.of(new VitamateAnalysisReaderPort.Citation(
                        1,
                        101L,
                        3001L,
                        4,
                        "security requirement excerpt"
                ))
        );
    }
}
