package com.group3.vitamins.project.presentation.api.request;

import com.group3.vitamins.project.application.command.UpdateProjectCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 프로젝트 수정 요청. 수정 화면이 폼 전체를 보내는 계약이라 받은 값으로 전부 덮어쓴다 —
 * 보내지 않은 필드는 유지가 아니라 <b>해제</b>다.
 */
@Schema(description = "프로젝트 수정 요청 — 폼 전체를 보낸다. 생략한 필드는 비워진다")
public record ProjectUpdateRequest(

        @NotBlank(message = "PROJECT_NAME_REQUIRED|과업명을 입력해 주세요.")
        @Size(max = 300, message = "PROJECT_NAME_TOO_LONG|과업명은 300자를 넘을 수 없습니다.")
        @Schema(description = "과업명 (최대 300자). 필수", example = "OO시 상수도 관리 용역")
        String name,

        @Schema(description = "설명", nullable = true)
        String description,

        @Size(max = 200, message = "CLIENT_NAME_TOO_LONG|발주처는 200자를 넘을 수 없습니다.")
        @Schema(description = "발주처 (최대 200자)", example = "OO시청", nullable = true)
        String clientName,

        @Schema(description = "시작일", example = "2026-08-01", nullable = true)
        LocalDate startedOn,

        @Schema(description = "종료일", example = "2027-01-31", nullable = true)
        LocalDate endedOn,

        @PositiveOrZero(message = "CONTRACT_AMOUNT_INVALID|계약금액은 0보다 작을 수 없습니다.")
        @Schema(description = "계약금액. 음수는 400", example = "135000000", nullable = true)
        BigDecimal contractAmount
) {

    public UpdateProjectCommand toCommand(Long projectId, String requesterUserId, String role) {
        return new UpdateProjectCommand(projectId, name, description, clientName,
                startedOn, endedOn, contractAmount, requesterUserId, role);
    }
}
