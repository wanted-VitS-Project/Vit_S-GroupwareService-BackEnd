package com.group3.vitamins.project.presentation.api.response;

import com.group3.vitamins.project.application.result.ProjectCloseResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "프로젝트 종결 응답")
public record ProjectCloseResponse(

        @Schema(description = "프로젝트 ID", example = "12")
        Long projectId,

        @Schema(description = "종결 후 상태", example = "CLOSED")
        String status,

        @Schema(description = "종결 사유 코드", example = "NOT_SELECTED")
        String closeReasonCode,

        @Schema(description = "종결 사유 상세", example = "기술평가 2순위로 탈락", nullable = true)
        String closeReasonNote,

        @Schema(description = "종결 일시", example = "2026-08-01T12:00:00")
        LocalDateTime closedAt
) {

    public static ProjectCloseResponse from(ProjectCloseResult result) {
        return new ProjectCloseResponse(result.projectId(), result.status(),
                result.closeReasonCode(), result.closeReasonNote(), result.closedAt());
    }
}
