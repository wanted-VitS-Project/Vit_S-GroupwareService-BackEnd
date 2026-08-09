package com.group3.vitamins.project.presentation.api.request;

import com.group3.vitamins.project.application.command.CloseProjectCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 프로젝트 종결 요청.
 *
 * <p>사유 코드의 <b>누락·오타 구분</b>은 서비스가 한다 — 명세가 두 상황에 다른 코드를 요구해서
 * {@code @NotBlank} 하나로는 표현할 수 없다 (PRJ-005).
 */
@Schema(description = "프로젝트 종결 요청")
public record ProjectCloseRequest(

        @Schema(description = "종결 사유 코드",
                allowableValues = {"NOT_PARTICIPATED", "FAILED_BID", "NOT_SELECTED", "CANCELED"},
                example = "NOT_SELECTED")
        String closeReasonCode,

        @Size(max = 500, message = "CLOSE_REASON_NOTE_TOO_LONG|종결 사유 상세는 500자를 넘을 수 없습니다.")
        @Schema(description = "종결 사유 상세 (최대 500자)",
                example = "기술평가 2순위로 탈락", nullable = true)
        String closeReasonNote
) {

    public CloseProjectCommand toCommand(Long projectId, String requesterUserId, String role) {
        return new CloseProjectCommand(projectId, closeReasonCode, closeReasonNote,
                requesterUserId, role);
    }
}
