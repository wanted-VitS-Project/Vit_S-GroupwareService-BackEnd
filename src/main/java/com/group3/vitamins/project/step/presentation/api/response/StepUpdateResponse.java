package com.group3.vitamins.project.step.presentation.api.response;

import com.group3.vitamins.project.step.application.result.StepUpdateResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "스텝 수정 응답")
public record StepUpdateResponse(

        @Schema(description = "스텝 ID", example = "10")
        Long stepId,

        @Schema(description = "스텝명", example = "제안서 작성·검토")
        String name,

        @Schema(description = "소속 스테이지 ID. 미소속이면 null (수정으로는 안 바뀐다)",
                example = "7", nullable = true)
        Long stageId,

        @Schema(description = "시작일", example = "2026-08-01", nullable = true)
        LocalDate startedOn,

        @Schema(description = "종료일", example = "2026-08-12", nullable = true)
        LocalDate endedOn,

        @Schema(description = "책임자. 해제 상태면 null", nullable = true)
        StepPersonResponse owner,

        @Schema(description = "수정 일시", example = "2026-08-01T13:00:00")
        LocalDateTime updatedAt
) {

    public static StepUpdateResponse from(StepUpdateResult result) {
        return new StepUpdateResponse(result.stepId(), result.name(), result.stageId(),
                result.startedOn(), result.endedOn(),
                result.owner() == null
                        ? null
                        : new StepPersonResponse(result.owner().userId(), result.owner().name()),
                result.updatedAt());
    }
}
