package com.group3.vitamins.project.presentation.api.response;

import com.group3.vitamins.project.application.result.ProjectStatusResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "프로젝트 상태 변경 응답")
public record ProjectStatusUpdateResponse(

        @Schema(description = "프로젝트 ID", example = "12")
        Long projectId,

        @Schema(description = "변경된 상태", example = "IN_PROGRESS")
        String status,

        @Schema(description = "변경 일시", example = "2026-08-01T11:30:00")
        LocalDateTime updatedAt
) {

    public static ProjectStatusUpdateResponse from(ProjectStatusResult result) {
        return new ProjectStatusUpdateResponse(result.projectId(), result.status(),
                result.updatedAt());
    }
}
