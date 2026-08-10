package com.group3.vitamins.project.step.presentation.api.response;

import com.group3.vitamins.project.step.application.result.StepDeleteResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스텝 삭제 응답")
public record StepDeleteResponse(

        @Schema(description = "삭제된 스텝 ID", example = "10")
        Long deletedStepId,

        @Schema(description = "다른 스텝으로 옮긴 블록 수", example = "2")
        int movedBlockCount,

        @Schema(description = "함께 삭제된 블록 수", example = "4")
        int deletedBlockCount,

        @Schema(description = "함께 삭제된 이슈 수", example = "3")
        int deletedIssueCount
) {

    public static StepDeleteResponse from(StepDeleteResult result) {
        return new StepDeleteResponse(result.deletedStepId(), result.movedBlockCount(),
                result.deletedBlockCount(), result.deletedIssueCount());
    }
}
