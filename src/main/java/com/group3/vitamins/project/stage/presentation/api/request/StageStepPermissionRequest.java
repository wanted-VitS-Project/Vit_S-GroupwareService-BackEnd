package com.group3.vitamins.project.stage.presentation.api.request;

import com.group3.vitamins.project.stage.application.command.ApplyStagePermissionCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 하위 스텝 권한 일괄 적용 요청 (STG-004).
 *
 * <p>기본값은 <b>항상</b> 저장된다. {@code applyToExistingSteps} 는 기존 스텝에도 지금 찍을지만 정한다.
 */
@Schema(description = "하위 스텝 권한 일괄 적용 요청")
public record StageStepPermissionRequest(

        @NotBlank(message = "USER_NOT_FOUND|대상 사원을 지정해 주세요.")
        @Schema(description = "대상 사원 사번", example = "E2024007")
        String userId,

        @NotBlank(message = "STEP_PERMISSION_INVALID|권한 등급을 입력해 주세요.")
        @Schema(description = "VIEWER · EDITOR · NONE", example = "EDITOR",
                allowableValues = {"VIEWER", "EDITOR", "NONE"})
        String permission,

        @Schema(description = "기존 하위 스텝에도 적용할지. 생략하면 true", example = "true",
                nullable = true)
        Boolean applyToExistingSteps
) {

    public ApplyStagePermissionCommand toCommand(Long stageId, String requesterUserId, String role) {
        return new ApplyStagePermissionCommand(
                stageId, userId, permission,
                applyToExistingSteps == null || applyToExistingSteps,
                requesterUserId, role);
    }
}
