package com.group3.vitamins.vitamate.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisReaderPort;
import com.group3.vitamins.vitamate.analysis.application.query.GetVitamateAnalysisJobQuery;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisJobDetailResult;
import com.group3.vitamins.vitamate.analysis.application.service.VitamateAnalysisJobQueryService;
import com.group3.vitamins.vitamate.analysis.domain.exception.VitamateErrorCode;
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
    @DisplayName("Python 작업 입력 조회")
    class GetAnalysisJob {

        @Test
        @DisplayName("PROCESSING 분석과 attemptId가 일치하면 Python 작업 입력을 반환한다")
        void returnsProcessingAnalysisJob() {
            VitamateAnalysisReaderPort.VitamateAnalysisJobDetail detail = analysisJobDetail();
            when(analysisReader.findProcessingAnalysisJob(ANALYSIS_ID, ATTEMPT_ID))
                    .thenReturn(Optional.of(detail));

            VitamateAnalysisJobDetailResult result = queryService.handle(query());

            assertThat(result.analysisId()).isEqualTo(ANALYSIS_ID);
            assertThat(result.attemptId()).isEqualTo(ATTEMPT_ID);
            assertThat(result.prompt()).isEqualTo("핵심 기술 요구사항과 위험 요소를 정리해줘.");
            assertThat(result.searchScope().projectId()).isEqualTo(10L);
            assertThat(result.searchScope().blockId()).isEqualTo(20L);
            assertThat(result.searchScope().fileVersionIds()).containsExactly(101L);
            assertThat(result.documents())
                    .hasSize(1)
                    .first()
                    .satisfies(document -> {
                        assertThat(document.fileVersionId()).isEqualTo(101L);
                        assertThat(document.fileName()).isEqualTo("제안요청서.pdf");
                        assertThat(document.chunks())
                                .hasSize(1)
                                .first()
                                .satisfies(chunk -> {
                                    assertThat(chunk.documentChunkId()).isEqualTo(3001L);
                                    assertThat(chunk.chromaId()).isEqualTo("fv101-chunk-1");
                                    assertThat(chunk.pageNumber()).isEqualTo(3);
                                    assertThat(chunk.excerpt()).isEqualTo("사업 범위는 통합 관제 플랫폼 구축이다.");
                                });
                    });
        }

        @Test
        @DisplayName("처리 가능한 분석 작업이 없으면 404 예외를 던진다")
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
    @DisplayName("입력값 검증")
    class ValidateInput {

        @Test
        @DisplayName("분석 ID가 없으면 조회 포트를 호출하지 않는다")
        void rejectsMissingAnalysisId() {
            assertThatThrownBy(() -> queryService.handle(new GetVitamateAnalysisJobQuery(null, ATTEMPT_ID)))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verify(analysisReader, never()).findProcessingAnalysisJob(null, ATTEMPT_ID);
        }

        @Test
        @DisplayName("분석 ID가 0 이하면 조회 포트를 호출하지 않는다")
        void rejectsNonPositiveAnalysisId() {
            assertThatThrownBy(() -> queryService.handle(new GetVitamateAnalysisJobQuery(0L, ATTEMPT_ID)))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verify(analysisReader, never()).findProcessingAnalysisJob(0L, ATTEMPT_ID);
        }

        @Test
        @DisplayName("attemptId가 비어 있으면 조회 포트를 호출하지 않는다")
        void rejectsBlankAttemptId() {
            assertThatThrownBy(() -> queryService.handle(new GetVitamateAnalysisJobQuery(ANALYSIS_ID, " ")))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verify(analysisReader, never()).findProcessingAnalysisJob(ANALYSIS_ID, " ");
        }
    }

    // 조회 테스트에서 사용할 query 값을 만든다.
    private GetVitamateAnalysisJobQuery query() {
        return new GetVitamateAnalysisJobQuery(ANALYSIS_ID, ATTEMPT_ID);
    }

    // 조회 성공 케이스에서 사용할 Python 작업 입력 값을 만든다.
    private VitamateAnalysisReaderPort.VitamateAnalysisJobDetail analysisJobDetail() {
        return new VitamateAnalysisReaderPort.VitamateAnalysisJobDetail(
                ANALYSIS_ID,
                ATTEMPT_ID,
                "핵심 기술 요구사항과 위험 요소를 정리해줘.",
                new VitamateAnalysisReaderPort.JobSearchScope(
                        10L,
                        20L,
                        List.of(101L)
                ),
                List.of(new VitamateAnalysisReaderPort.JobDocument(
                        101L,
                        "제안요청서.pdf",
                        List.of(new VitamateAnalysisReaderPort.JobChunk(
                                3001L,
                                "fv101-chunk-1",
                                3,
                                "사업 범위는 통합 관제 플랫폼 구축이다."
                        ))
                ))
        );
    }
}
