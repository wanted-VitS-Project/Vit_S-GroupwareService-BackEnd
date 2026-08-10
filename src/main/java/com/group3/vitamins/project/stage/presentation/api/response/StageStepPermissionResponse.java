package com.group3.vitamins.project.stage.presentation.api.response;

import com.group3.vitamins.project.stage.application.result.StageStepPermissionResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "하위 스텝 권한 일괄 적용 응답")
public record StageStepPermissionResponse(

        @Schema(description = "스테이지 ID", example = "7")
        Long stageId,

        @Schema(description = "사원 사번", example = "E2024007")
        String userId,

        @Schema(description = "적용된 권한 등급", example = "EDITOR")
        String permission,

        @Schema(description = "권한이 적용된 기존 스텝 수. applyToExistingSteps=false 면 0", example = "3")
        int appliedStepCount
) {

    public static StageStepPermissionResponse from(StageStepPermissionResult result) {
        return new StageStepPermissionResponse(
                result.stageId(), result.userId(), result.permission(), result.appliedStepCount());
    }
}
