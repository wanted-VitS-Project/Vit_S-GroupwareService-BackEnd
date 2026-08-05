package com.group3.vitamins.project.block.presentation.api.response;

import com.group3.vitamins.project.block.application.result.BlockUpdateResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "블록 제목·담당자 수정 응답")
public record BlockUpdateResponse(

        @Schema(description = "블록 ID", example = "16")
        Long blockId,

        @Schema(description = "반영된 제목. 해제했으면 null", example = "핵심 요구사항 메모")
        String title,

        @Schema(description = "반영된 담당자. 해제했거나 미지정이면 null")
        BlockOwnerResponse owner,

        @Schema(description = "수정 일시")
        LocalDateTime updatedAt
) {

    /** 수정 결과를 응답으로 옮긴다. */
    public static BlockUpdateResponse from(BlockUpdateResult result) {
        BlockOwnerResponse owner = result.owner() == null
                ? null
                : new BlockOwnerResponse(result.owner().userId(), result.owner().name());

        return new BlockUpdateResponse(
                result.blockId(), result.title(), owner, result.updatedAt());
    }
}