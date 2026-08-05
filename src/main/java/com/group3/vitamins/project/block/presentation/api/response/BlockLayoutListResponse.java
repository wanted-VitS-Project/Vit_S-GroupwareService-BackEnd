package com.group3.vitamins.project.block.presentation.api.response;

import com.group3.vitamins.project.block.application.result.BlockLayoutResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "블록 배치 변경 응답")
public record BlockLayoutListResponse(

        @Schema(description = "반영된 배치 (요청 순서)")
        List<BlockLayoutItemResponse> blocks
) {

    /** 배치 결과를 응답으로 옮긴다. */
    public static BlockLayoutListResponse from(List<BlockLayoutResult> results) {
        return new BlockLayoutListResponse(results.stream()
                .map(BlockLayoutItemResponse::from)
                .toList());
    }

    @Schema(description = "반영된 배치 한 건")
    public record BlockLayoutItemResponse(

            @Schema(description = "블록 ID", example = "15")
            Long blockId,

            @Schema(description = "행 인덱스", example = "0")
            int rowIndex,

            @Schema(description = "행 내 순서", example = "0")
            int sortOrder,

            @Schema(description = "열 병합 수", example = "2")
            int colSpan
    ) {

        static BlockLayoutItemResponse from(BlockLayoutResult result) {
            return new BlockLayoutItemResponse(
                    result.blockId(), result.rowIndex(), result.sortOrder(), result.colSpan());
        }
    }
}