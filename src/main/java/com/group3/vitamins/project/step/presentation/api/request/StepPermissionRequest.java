package com.group3.vitamins.project.step.presentation.api.request;

import com.group3.vitamins.project.step.application.command.SetStepPermissionCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 스텝 권한 부여·변경 요청.
 * 특정 스텝만 가리려면 {@code NONE} 을 명시적으로 넣어야 한다 — 행을 안 만들면 상속이지 차단이 아니다.
 */
@Schema(description = "스텝 권한 부여·변경 요청")
public record StepPermissionRequest(

        @NotBlank(message = "STEP_PERMISSION_INVALID|권한 등급을 입력해 주세요.")
        @Schema(description = "VIEWER · EDITOR · NONE", example = "NONE",
                allowableValues = {"VIEWER", "EDITOR", "NONE"})
        String permission
) {

    public SetStepPermissionCommand toCommand(Long stepId, String userId,
                                              String requesterUserId, String role) {
        return new SetStepPermissionCommand(stepId, userId, permission, requesterUserId, role);
    }
}
