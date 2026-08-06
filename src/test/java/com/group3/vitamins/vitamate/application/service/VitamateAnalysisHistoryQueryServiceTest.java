package com.group3.vitamins.vitamate.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisReaderPort;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateBlockReaderPort;
import com.group3.vitamins.vitamate.analysis.application.query.GetVitamateBlockAnalysisHistoryQuery;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisHistoryResult;
import com.group3.vitamins.vitamate.analysis.application.service.VitamateAnalysisHistoryQueryService;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("VitamateAnalysisHistoryQueryService")
class VitamateAnalysisHistoryQueryServiceTest {

    private static final Long BLOCK_ID = 10L;
    private static final Long VITAMATE_BLOCK_ID = 20L;
    private static final Long STEP_ID = 30L;
    private static final Long PROJECT_ID = 40L;
    private static final String USER_ID = "EMP001";
    private static final int HISTORY_LIMIT = 20;

    private VitamateBlockReaderPort blockReader;
    private VitamateAnalysisReaderPort analysisReader;
    private VitamateAnalysisHistoryQueryService queryService;

    @BeforeEach
    void setUp() {
        blockReader = mock(VitamateBlockReaderPort.class);
        analysisReader = mock(VitamateAnalysisReaderPort.class);
        queryService = new VitamateAnalysisHistoryQueryService(blockReader, analysisReader);
    }

    @Nested
    @DisplayName("get block analysis histories")
    class GetBlockAnalysisHistories {

        @Test
        @DisplayName("returns histories for an accessible Vitamate block")
        void returnsHistoriesForAccessibleVitamateBlock() {
            when(blockReader.findAccessibleVitamateBlock(BLOCK_ID, USER_ID))
                    .thenReturn(Optional.of(blockContext()));
            when(analysisReader.findBlockAnalysisHistories(VITAMATE_BLOCK_ID, HISTORY_LIMIT))
                    .thenReturn(List.of(history(1L), history(2L)));

            VitamateAnalysisHistoryResult result = queryService.handle(query());

            assertThat(result.blockId()).isEqualTo(BLOCK_ID);
            assertThat(result.content())
                    .hasSize(2)
                    .extracting(VitamateAnalysisHistoryResult.Item::analysisId)
                    .containsExactly(1L, 2L);
            verify(analysisReader).findBlockAnalysisHistories(VITAMATE_BLOCK_ID, HISTORY_LIMIT);
        }

        @Test
        @DisplayName("returns empty content when no analysis history exists")
        void returnsEmptyContentWhenNoAnalysisHistoryExists() {
            when(blockReader.findAccessibleVitamateBlock(BLOCK_ID, USER_ID))
                    .thenReturn(Optional.of(blockContext()));
            when(analysisReader.findBlockAnalysisHistories(VITAMATE_BLOCK_ID, HISTORY_LIMIT))
                    .thenReturn(List.of());

            VitamateAnalysisHistoryResult result = queryService.handle(query());

            assertThat(result.blockId()).isEqualTo(BLOCK_ID);
            assertThat(result.content()).isEmpty();
        }

        @Test
        @DisplayName("throws not found when block is not accessible")
        void throwsNotFoundWhenBlockIsNotAccessible() {
            when(blockReader.findAccessibleVitamateBlock(BLOCK_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryService.handle(query()))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_BLOCK_NOT_FOUND));

            verify(analysisReader, never()).findBlockAnalysisHistories(VITAMATE_BLOCK_ID, HISTORY_LIMIT);
        }
    }

    @Nested
    @DisplayName("input validation")
    class ValidateInput {

        @Test
        @DisplayName("rejects missing block id")
        void rejectsMissingBlockId() {
            assertInvalid(new GetVitamateBlockAnalysisHistoryQuery(null, USER_ID));
        }

        @Test
        @DisplayName("rejects missing query")
        void rejectsMissingQuery() {
            assertThatThrownBy(() -> queryService.handle(null))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verifyNoInteractions(blockReader, analysisReader);
        }

        @Test
        @DisplayName("rejects non-positive block id")
        void rejectsNonPositiveBlockId() {
            assertInvalid(new GetVitamateBlockAnalysisHistoryQuery(0L, USER_ID));
        }

        @Test
        @DisplayName("rejects blank user id")
        void rejectsBlankUserId() {
            assertInvalid(new GetVitamateBlockAnalysisHistoryQuery(BLOCK_ID, " "));
        }

        // 입력값이 잘못되면 블록 조회와 이력 조회를 모두 시작하지 않습니다.
        private void assertInvalid(GetVitamateBlockAnalysisHistoryQuery query) {
            assertThatThrownBy(() -> queryService.handle(query))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                            .isEqualTo(VitamateErrorCode.VITAMATE_INVALID_REQUEST));

            verifyNoInteractions(blockReader, analysisReader);
        }
    }

    // 테스트에서 반복 사용하는 분석 이력 조회 Query를 만듭니다.
    private GetVitamateBlockAnalysisHistoryQuery query() {
        return new GetVitamateBlockAnalysisHistoryQuery(BLOCK_ID, USER_ID);
    }

    // 접근 가능한 비타메이트 블록 컨텍스트를 만듭니다.
    private VitamateBlockReaderPort.VitamateBlockContext blockContext() {
        return new VitamateBlockReaderPort.VitamateBlockContext(
                BLOCK_ID,
                VITAMATE_BLOCK_ID,
                STEP_ID,
                PROJECT_ID
        );
    }

    // 분석 이력 목록에 들어갈 Reader Port 값을 만듭니다.
    private VitamateAnalysisReaderPort.VitamateAnalysisHistory history(Long analysisId) {
        return new VitamateAnalysisReaderPort.VitamateAnalysisHistory(
                analysisId,
                "핵심 기술 요구사항과 위험 요소를 정리해줘.",
                "COMPLETED",
                LocalDateTime.of(2026, 8, 4, 14, 5),
                LocalDateTime.of(2026, 8, 4, 14, 8)
        );
    }
}
