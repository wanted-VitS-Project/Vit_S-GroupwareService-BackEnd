package com.group3.vitamins.project.step.presentation.api.request;

import com.group3.vitamins.project.step.application.command.UpdateStepCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 스텝 수정 요청. 편집 화면이 폼 전체를 보내는 계약이라 받은 값으로 전부 덮어쓴다 —
 * 보내지 않은 필드는 유지가 아니라 <b>해제</b>다.
 *
 * <p>소속 스테이지는 받지 않는다. 스테이지 이동·순서는 {@code PATCH .../steps/order} 소관이다.
 */
@Schema(description = "스텝 수정 요청 — 이름·기간·책임자. 위치 변경은 순서 API 를 쓴다")
public record StepUpdateRequest(

        @NotBlank(message = "STEP_NAME_REQUIRED|스텝명을 입력해 주세요.")
        @Size(max = 200, message = "STEP_NAME_TOO_LONG|스텝명은 200자를 넘을 수 없습니다.")
        @Schema(description = "스텝명 (최대 200자)", example = "제안서 작성·검토")
        String name,

        @Schema(description = "시작일", example = "2026-08-01", nullable = true)
        LocalDate startedOn,

        @Schema(description = "종료일", example = "2026-08-12", nullable = true)
        LocalDate endedOn,

        @Schema(description = "책임자 사번. 생략하면 책임자를 해제한다",
                example = "E2024007", nullable = true)
        String ownerUserId,

        @NotNull(message = "STEP_VERSION_REQUIRED|버전 정보가 없습니다. 화면을 새로고침해 주세요.")
        @Schema(description = "조회에서 받은 version 을 그대로 실어 보낸다. "
                + "그 사이 남이 먼저 저장했으면 409 다", example = "7")
        Integer version,

        @Schema(description = "true 면 충돌을 무시하고 덮어쓴다. 생략하면 false", example = "false")
        Boolean overwrite
) {

    /** ⚠️ overwrite 는 선택 필드라 null 이 온다. {@code Boolean.TRUE.equals} 로 받아야 NPE 가 안 난다. */
    public UpdateStepCommand toCommand(Long stepId, String requesterUserId, String role) {
        return new UpdateStepCommand(stepId, name, startedOn, endedOn, ownerUserId,
                version, Boolean.TRUE.equals(overwrite), requesterUserId, role);
    }
}
