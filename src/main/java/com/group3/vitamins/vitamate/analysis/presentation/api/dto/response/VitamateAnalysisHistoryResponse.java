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

            @Schema(description = "검토 유형", example = "COST_REPORT")
            String reviewType,

            @Schema(description = "선택한 검토 카테고리 코드 목록", example = "[\"COMMON\", \"COST_RESULT\"]")
            List<String> reviewCategoryCodes,

            @Schema(description = "분석에 사용한 최종 프롬프트", example = "기준 문서와 비교하여 금액과 부가세 포함 여부를 확인해주세요.")
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
                    item.reviewType(),
                    item.reviewCategoryCodes(),
                    item.prompt(),
                    item.analysisStatus(),
                    item.createdAt(),
                    item.completedAt()
            );
        }
    }
}
