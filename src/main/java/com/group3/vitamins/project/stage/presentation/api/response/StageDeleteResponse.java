package com.group3.vitamins.project.stage.presentation.api.response;

import com.group3.vitamins.project.stage.application.result.StageDeleteResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스테이지 삭제 응답")
public record StageDeleteResponse(

        @Schema(description = "삭제된 스테이지 ID", example = "7")
        Long deletedStageId,

        @Schema(description = "이전된 스텝 수", example = "3")
        int movedStepCount,

        @Schema(description = "이전 대상 스테이지 ID. null 이면 미소속으로 뺐다", example = "8",
                nullable = true)
        Long moveToStageId
) {

    public static StageDeleteResponse from(StageDeleteResult result) {
        return new StageDeleteResponse(
                result.deletedStageId(), result.movedStepCount(), result.moveToStageId());
    }
}
