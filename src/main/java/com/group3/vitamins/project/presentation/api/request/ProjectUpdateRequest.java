package com.group3.vitamins.project.presentation.api.request;

import com.group3.vitamins.project.application.command.UpdateProjectCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
        BigDecimal contractAmount,

        @NotNull(message = "PROJECT_VERSION_REQUIRED|버전 정보가 없습니다. 화면을 새로고침해 주세요.")
        @Schema(description = "상세 조회에서 받은 version 을 그대로 실어 보낸다. "
                + "그 사이 남이 먼저 저장했으면 409 다", example = "7")
        Integer version,

        @Schema(description = "true 면 충돌을 무시하고 덮어쓴다. 생략하면 false", example = "false")
        Boolean overwrite
) {

    /** ⚠️ overwrite 는 선택 필드라 null 이 온다. {@code Boolean.TRUE.equals} 로 받아야 NPE 가 안 난다. */
    public UpdateProjectCommand toCommand(Long projectId, String requesterUserId, String role) {
        return new UpdateProjectCommand(projectId, name, description, clientName,
                startedOn, endedOn, contractAmount, version, Boolean.TRUE.equals(overwrite),
                requesterUserId, role);
    }
}
