package com.group3.vitamins.project.block.presentation.api.request;

import com.group3.vitamins.project.block.application.command.MoveBlockCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 블록 이동 요청 (BLK-014).
 * 같은 프로젝트의 다른 스텝으로만 옮길 수 있고, {@code issue_block} 연결은 끊긴다.
 */
@Schema(description = "블록 이동 요청")
public record BlockMoveRequest(

        @NotNull(message = "BLOCK_MOVE_TARGET_REQUIRED|블록을 옮길 스텝을 지정해 주세요.")
        @Schema(description = "옮길 대상 스텝 ID (같은 프로젝트)", example = "11")
        Long stepId
) {

    public MoveBlockCommand toCommand(Long blockId, String requesterUserId, String role) {
        return new MoveBlockCommand(blockId, stepId, requesterUserId, role);
    }
}
