package com.group3.vitamins.project.presentation.api.response;

import com.group3.vitamins.project.application.result.ProjectUpdateResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "프로젝트 수정 응답")
public record ProjectUpdateResponse(

        @Schema(description = "프로젝트 ID", example = "12")
        Long projectId,

        @Schema(description = "과업명", example = "OO시 상수도 관리 용역")
        String name,

        @Schema(description = "발주처", example = "OO시청")
        String clientName,

        @Schema(description = "시작일", example = "2026-08-01")
        LocalDate startedOn,

        @Schema(description = "종료일", example = "2027-01-31")
        LocalDate endedOn,

        @Schema(description = "계약금액", example = "135000000")
        BigDecimal contractAmount,

        @Schema(description = "수정 일시", example = "2026-08-01T11:20:00")
        LocalDateTime updatedAt
) {

    public static ProjectUpdateResponse from(ProjectUpdateResult result) {
        return new ProjectUpdateResponse(result.projectId(), result.name(), result.clientName(),
                result.startedOn(), result.endedOn(), result.contractAmount(), result.updatedAt());
    }
}