package com.group3.vitamins.project.stage.presentation.api.response;

import com.group3.vitamins.project.stage.application.result.StageResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스테이지 생성 응답")
public record StageCreateResponse(

        @Schema(description = "생성된 스테이지 ID", example = "7")
        Long stageId,

        @Schema(description = "프로젝트 ID", example = "12")
        Long projectId,

        @Schema(description = "스테이지명", example = "제안")
        String name,

        @Schema(description = "정렬 순서", example = "1")
        int sortOrder
) {

    /** 생성 결과를 응답으로 옮긴다. */
    public static StageCreateResponse from(StageResult result) {
        return new StageCreateResponse(
                result.stageId(), result.projectId(), result.name(), result.sortOrder());
    }
}