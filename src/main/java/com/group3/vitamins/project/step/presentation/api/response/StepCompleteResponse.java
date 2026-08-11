package com.group3.vitamins.project.step.presentation.api.response;

import com.group3.vitamins.project.step.application.result.StepCompleteResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "스텝 완료 처리 응답")
public record StepCompleteResponse(

        @Schema(description = "스텝 ID", example = "10")
        Long stepId,

        @Schema(description = "변경된 상태. 항상 DONE", example = "DONE")
        String status,

        @Schema(description = "완료 시점의 미완료 이슈 수", example = "3")
        int openIssueCount,

        @Schema(description = "적용된 처리 방식", example = "KEEP")
        String openIssueAction,

        @Schema(description = "함께 종료된 이슈 수. KEEP 이면 0", example = "0")
        int closedIssueCount,

        @Schema(description = "완료자")
        StepPersonResponse completedBy,

        @Schema(description = "완료 시각", example = "2026-08-10T17:00:00")
        LocalDateTime completedAt
) {

    public static StepCompleteResponse from(StepCompleteResult result) {
        return new StepCompleteResponse(
                result.stepId(), result.status(), result.openIssueCount(),
                result.openIssueAction(), result.closedIssueCount(),
                result.completedBy() == null
                        ? null
                        : new StepPersonResponse(
                                result.completedBy().userId(), result.completedBy().name(), result.completedBy().deleted()),
                result.completedAt());
    }
}
