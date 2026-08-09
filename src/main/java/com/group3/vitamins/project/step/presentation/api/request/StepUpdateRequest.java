package com.group3.vitamins.project.step.presentation.api.request;

import com.group3.vitamins.project.step.application.command.UpdateStepCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
        String ownerUserId
) {

    public UpdateStepCommand toCommand(Long stepId, String requesterUserId, String role) {
        return new UpdateStepCommand(stepId, name, startedOn, endedOn, ownerUserId,
                requesterUserId, role);
    }
}
