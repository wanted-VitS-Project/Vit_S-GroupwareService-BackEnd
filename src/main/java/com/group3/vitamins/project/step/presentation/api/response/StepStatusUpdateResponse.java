package com.group3.vitamins.project.step.presentation.api.response;

import com.group3.vitamins.project.step.application.result.StepStatusResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "스텝 상태 변경 응답")
public record StepStatusUpdateResponse(

        @Schema(description = "스텝 ID", example = "10")
        Long stepId,

        @Schema(description = "변경된 상태", example = "IN_PROGRESS")
        String status,

        @Schema(description = "변경 일시", example = "2026-08-01T13:10:00")
        LocalDateTime updatedAt
) {

    public static StepStatusUpdateResponse from(StepStatusResult result) {
        return new StepStatusUpdateResponse(result.stepId(), result.status(), result.updatedAt());
    }
}
