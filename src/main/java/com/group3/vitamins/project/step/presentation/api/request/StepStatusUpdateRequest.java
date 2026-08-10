package com.group3.vitamins.project.step.presentation.api.request;

import com.group3.vitamins.project.step.application.command.ChangeStepStatusCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 스텝 상태 변경 요청. DONE 은 허용값이 아니다 — 완료는 별도 API 다. */
@Schema(description = "스텝 상태 변경 요청")
public record StepStatusUpdateRequest(

        @NotBlank(message = "STEP_STATUS_INVALID|상태 값을 입력해 주세요.")
        @Schema(description = "NOT_STARTED · IN_PROGRESS", example = "IN_PROGRESS",
                allowableValues = {"NOT_STARTED", "IN_PROGRESS"})
        String status,

        @NotNull(message = "STEP_VERSION_REQUIRED|버전 정보가 없습니다. 화면을 새로고침해 주세요.")
        @Schema(description = "조회에서 받은 version 을 그대로 실어 보낸다", example = "7")
        Integer version,

        @Schema(description = "true 면 충돌을 무시하고 덮어쓴다. 생략하면 false", example = "false")
        Boolean overwrite
) {

    /** ⚠️ overwrite 는 선택 필드라 null 이 온다. {@code Boolean.TRUE.equals} 로 받아야 NPE 가 안 난다. */
    public ChangeStepStatusCommand toCommand(Long stepId, String requesterUserId, String role) {
        return new ChangeStepStatusCommand(
                stepId, status, version, Boolean.TRUE.equals(overwrite), requesterUserId, role);
    }
}
