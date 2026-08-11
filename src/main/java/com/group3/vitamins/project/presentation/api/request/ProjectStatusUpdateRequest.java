package com.group3.vitamins.project.presentation.api.request;

import com.group3.vitamins.project.application.command.ChangeProjectStatusCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 프로젝트 상태 변경 요청.
 *
 * <p>{@code status} 는 문자열로 받는다 — enum 으로 바인딩하면 잘못된 값이 역직렬화 단계에서
 * {@code COMMON_INVALID_REQUEST} 로 새서 명세의 {@code PROJECT_STATUS_INVALID} 를 못 내린다.
 * {@code CLOSED} 금지는 값 형식이 아니라 API 규칙이라 서비스가 판정한다.
 */
@Schema(description = "프로젝트 상태 변경 요청")
public record ProjectStatusUpdateRequest(

        @Schema(description = "변경할 상태. CLOSED 는 종결 API 를 쓴다",
                allowableValues = {"NOT_STARTED", "IN_PROGRESS", "SETTLEMENT", "COMPLETED"},
                example = "IN_PROGRESS")
        String status,

        @NotNull(message = "PROJECT_VERSION_REQUIRED|버전 정보가 없습니다. 화면을 새로고침해 주세요.")
        @Schema(description = "상세 조회에서 받은 version 을 그대로 실어 보낸다", example = "7")
        Integer version,

        @Schema(description = "true 면 충돌을 무시하고 덮어쓴다. 생략하면 false", example = "false")
        Boolean overwrite
) {

    /** ⚠️ overwrite 는 선택 필드라 null 이 온다. {@code Boolean.TRUE.equals} 로 받아야 NPE 가 안 난다. */
    public ChangeProjectStatusCommand toCommand(Long projectId, String requesterUserId, String role) {
        return new ChangeProjectStatusCommand(projectId, status, version,
                Boolean.TRUE.equals(overwrite), requesterUserId, role);
    }
}
