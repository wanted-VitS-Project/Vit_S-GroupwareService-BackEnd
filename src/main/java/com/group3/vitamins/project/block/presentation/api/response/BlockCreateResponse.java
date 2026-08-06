    package com.group3.vitamins.project.block.presentation.api.response;

import com.group3.vitamins.project.block.application.result.BlockResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "블록 생성 응답")
public record BlockCreateResponse(

        @Schema(description = "생성된 블록 ID", example = "21")
        Long blockId,

        @Schema(description = "소속 스텝 ID", example = "10")
        Long stepId,

        @Schema(description = "소속 프로젝트 ID", example = "12")
        Long projectId,

        @Schema(description = "블록 타입", example = "CHECKLIST")
        String type,

        @Schema(description = "블록 제목. 안 보냈으면 null", example = "제출 서류 점검", nullable = true)
        String title,

        @Schema(description = "블록 담당자. 안 보냈으면 null")
        BlockOwnerResponse owner,

        @Schema(description = "행 인덱스", example = "0")
        int rowIndex,

        @Schema(description = "행 내 순서", example = "1")
        int sortOrder,

        @Schema(description = "열 병합 수", example = "1")
        int colSpan,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {

    /** 생성 결과를 응답으로 옮긴다. */
    public static BlockCreateResponse from(BlockResult result) {
        BlockOwnerResponse owner = result.owner() == null
                ? null
                : new BlockOwnerResponse(result.owner().userId(), result.owner().name());

        return new BlockCreateResponse(
                result.blockId(), result.stepId(), result.projectId(), result.type(),
                result.title(), owner, result.rowIndex(), result.sortOrder(),
                result.colSpan(), result.createdAt());
    }
}