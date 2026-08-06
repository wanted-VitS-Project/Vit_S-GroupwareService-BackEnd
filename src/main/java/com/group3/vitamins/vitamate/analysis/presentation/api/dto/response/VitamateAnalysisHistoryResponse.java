package com.group3.vitamins.vitamate.analysis.presentation.api.dto.response;

import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisHistoryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

// 비타메이트 블록별 분석 실행 이력 조회 응답입니다.
@Schema(description = "비타메이트 블록별 분석 실행 이력 조회 응답")
public record VitamateAnalysisHistoryResponse(

        @Schema(description = "비타메이트 블록 ID", example = "12")
        Long blockId,

        @Schema(description = "분석 실행 이력 목록. 이력이 없으면 빈 배열")
        List<Item> content
) {

    // application result를 HTTP 응답 DTO로 변환합니다.
    public static VitamateAnalysisHistoryResponse from(VitamateAnalysisHistoryResult result) {
        return new VitamateAnalysisHistoryResponse(
                result.blockId(),
                result.content().stream()
                        .map(Item::from)
                        .toList()
        );
    }

    public record Item(
            @Schema(description = "분석 ID", example = "501")
            Long analysisId,

            @Schema(description = "분석 프롬프트", example = "핵심 기술 요구사항과 위험 요소를 정리해줘.")
            String prompt,

            @Schema(description = "분석 상태", example = "COMPLETED")
            String analysisStatus,

            @Schema(description = "요청 시각", example = "2026-08-04T14:05:00")
            LocalDateTime createdAt,

            @Schema(description = "완료 시각. 완료 전이면 null", example = "2026-08-04T14:07:00")
            LocalDateTime completedAt
    ) {

        // application result item을 HTTP 응답 item으로 변환합니다.
        private static Item from(VitamateAnalysisHistoryResult.Item item) {
            return new Item(
                    item.analysisId(),
                    item.prompt(),
                    item.analysisStatus(),
                    item.createdAt(),
                    item.completedAt()
            );
        }
    }
}