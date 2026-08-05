package com.group3.vitamins.vitamate.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.application.port.VitamateAnalysisReaderPort;
import com.group3.vitamins.vitamate.application.query.GetVitamateAnalysisQuery;
import com.group3.vitamins.vitamate.application.result.VitamateAnalysisDetailResult;
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
    @DisplayName("분석 상세 조회")
    class GetAnalysis {

        @Test
        @DisplayName("접근 가능한 분석이면 상태, 문서, 근거를 반환한다")
        void returnsAccessibleAnalysisDetail() {
            VitamateAnalysisReaderPort.VitamateAnalysisDetail detail = analysisDetail();
            when(analysisReader.findAccessibleAnalysis(ANALYSIS_ID, USER_ID))
                    .thenReturn(Optional.of(detail));

            VitamateAnalysisDetailResult result = queryService.handle(query());

            assertThat(result.analysisId()).isEqualTo(ANALYSIS_ID);
            assertThat(result.blockId()).isEqualTo(10L);
            assertThat(result.prompt()).isEqualTo("핵심 기술 요구사항과 위험 요소를 정리해줘.");
            assertThat(result.analysisStatus()).isEqualTo("COMPLETED");
            assertThat(result.result()).isEqualTo("analysis result");
            assertThat(result.errorMessage()).isNull();
            assertThat(result.documents())
                    .hasSize(1)
                    .first()
                    .satisfies(document -> {
                        assertThat(document.fileVersionId()).isEqualTo(101L);
                        assertThat(document.fileName()).isEqualTo("제안요청서.pdf");
                    });
            assertThat(result.citations())
                    .hasSize(1)
                    .first()
                    .satisfies(citation -> {
                        assertThat(citation.rankOrder()).isEqualTo(1);
                        assertThat(citation.fileVersionId()).isEqualTo(101L);
                        assertThat(citation.documentChunkId()).isEqualTo(3001L);
                        assertThat(citation.pageNumber()).isEqualTo(4);
                        assertThat(citation.excerpt()).isEqualTo("통합 관제 플랫폼 구축");
                    });
        }

        @Test
        @DisplayName("분석이 없거나 접근할 수 없으면 404 예외를 던진다")
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
    @DisplayName("입력값 검증")
    class ValidateInput {

        @Test
        @DisplayName("분석 ID가 없으면 조회 포트를 호출하지 않는다")
        void rejectsMissingAnalysisId() {
            assertThatThrownBy(() -> queryService.handle(new GetVitamateAnalysisQuery(null, USER_ID)))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verify(analysisReader, never()).findAccessibleAnalysis(null, USER_ID);
        }

        @Test
        @DisplayName("분석 ID가 0 이하이면 조회 포트를 호출하지 않는다")
        void rejectsNonPositiveAnalysisId() {
            assertThatThrownBy(() -> queryService.handle(new GetVitamateAnalysisQuery(0L, USER_ID)))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verify(analysisReader, never()).findAccessibleAnalysis(0L, USER_ID);
        }

        @Test
        @DisplayName("사용자 ID가 비어 있으면 조회 포트를 호출하지 않는다")
        void rejectsBlankUserId() {
            assertThatThrownBy(() -> queryService.handle(new GetVitamateAnalysisQuery(ANALYSIS_ID, " ")))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verify(analysisReader, never()).findAccessibleAnalysis(ANALYSIS_ID, " ");
        }
    }

    // 조회 테스트에서 사용할 query 값을 만든다.
    private GetVitamateAnalysisQuery query() {
        return new GetVitamateAnalysisQuery(ANALYSIS_ID, USER_ID);
    }

    // 조회 성공 케이스에서 사용할 분석 상세 값을 만든다.
    private VitamateAnalysisReaderPort.VitamateAnalysisDetail analysisDetail() {
        return new VitamateAnalysisReaderPort.VitamateAnalysisDetail(
                ANALYSIS_ID,
                10L,
                "핵심 기술 요구사항과 위험 요소를 정리해줘.",
                "COMPLETED",
                "analysis result",
                null,
                LocalDateTime.of(2026, 8, 4, 14, 5),
                LocalDateTime.of(2026, 8, 4, 14, 8),
                List.of(new VitamateAnalysisReaderPort.Document(
                        101L,
                        "제안요청서.pdf"
                )),
                List.of(new VitamateAnalysisReaderPort.Citation(
                        1,
                        101L,
                        3001L,
                        4,
                        "통합 관제 플랫폼 구축"
                ))
        );
    }
}
