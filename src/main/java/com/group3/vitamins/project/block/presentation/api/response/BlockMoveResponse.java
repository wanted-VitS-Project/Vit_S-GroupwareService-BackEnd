package com.group3.vitamins.project.block.presentation.api.response;

import com.group3.vitamins.project.block.application.result.BlockMoveResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "블록 이동 응답")
public record BlockMoveResponse(

        @Schema(description = "블록 ID", example = "21")
        Long blockId,

        @Schema(description = "옮겨진 스텝 ID", example = "11")
        Long stepId,

        @Schema(description = "이동 때문에 끊긴 이슈-블록 연결 수", example = "2")
        int unlinkedIssueCount
) {

    public static BlockMoveResponse from(BlockMoveResult result) {
        return new BlockMoveResponse(
                result.blockId(), result.stepId(), result.unlinkedIssueCount());
    }
}
