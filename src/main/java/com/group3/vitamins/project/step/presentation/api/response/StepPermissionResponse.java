package com.group3.vitamins.project.step.presentation.api.response;

import com.group3.vitamins.project.step.application.result.StepPermissionResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스텝 권한 부여·회수 응답")
public record StepPermissionResponse(

        @Schema(description = "스텝 ID", example = "10")
        Long stepId,

        @Schema(description = "사원 사번", example = "E2024007")
        String userId,

        @Schema(description = "적용된 권한 등급. 회수 응답에서는 상속으로 되돌아간 등급", example = "NONE")
        String permission,

        @Schema(description = "오버라이드 행 보유 여부", example = "true")
        boolean overridden
) {

    public static StepPermissionResponse from(StepPermissionResult result) {
        return new StepPermissionResponse(
                result.stepId(), result.userId(), result.permission(), result.overridden());
    }
}
