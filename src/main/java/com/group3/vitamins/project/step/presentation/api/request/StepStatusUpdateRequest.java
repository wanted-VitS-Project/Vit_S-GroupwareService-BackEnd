package com.group3.vitamins.project.step.presentation.api.request;

import com.group3.vitamins.project.step.application.command.ChangeStepStatusCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 스텝 상태 변경 요청. DONE 은 허용값이 아니다 — 완료는 별도 API 다. */
@Schema(description = "스텝 상태 변경 요청")
public record StepStatusUpdateRequest(

        @NotBlank(message = "STEP_STATUS_INVALID|상태 값을 입력해 주세요.")
        @Schema(description = "NOT_STARTED · IN_PROGRESS", example = "IN_PROGRESS",
                allowableValues = {"NOT_STARTED", "IN_PROGRESS"})
        String status
) {

    public ChangeStepStatusCommand toCommand(Long stepId, String requesterUserId, String role) {
        return new ChangeStepStatusCommand(stepId, status, requesterUserId, role);
    }
}
