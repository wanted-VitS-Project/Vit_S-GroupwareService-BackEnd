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
        Long stepId,

        @NotNull(message = "BLOCK_VERSION_REQUIRED|버전 정보가 없습니다. 화면을 새로고침해 주세요.")
        @Schema(description = "조회에서 받은 version 을 그대로 실어 보낸다", example = "7")
        Integer version,

        @Schema(description = "true 면 충돌을 무시하고 덮어쓴다. 생략하면 false", example = "false")
        Boolean overwrite
) {

    /** ⚠️ overwrite 는 선택 필드라 null 이 온다. {@code Boolean.TRUE.equals} 로 받아야 NPE 가 안 난다. */
    public MoveBlockCommand toCommand(Long blockId, String requesterUserId, String role) {
        return new MoveBlockCommand(blockId, stepId, version,
                Boolean.TRUE.equals(overwrite), requesterUserId, role);
    }
}
