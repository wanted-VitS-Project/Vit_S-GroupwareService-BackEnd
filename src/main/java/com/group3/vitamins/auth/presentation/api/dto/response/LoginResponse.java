package com.group3.vitamins.auth.presentation.api.dto.response;

import com.group3.vitamins.auth.infrastructure.persistence.UserProfileRow;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그인 응답.
 *
 * <p>⚠️ <b>토큰을 담지 않는다.</b> 인증은 HttpOnly 세션 쿠키로만 전달한다 (`.ai/api/auth.md`).
 */
@Schema(description = "로그인 응답")
public record LoginResponse(

        @Schema(description = "사번", example = "EMP001")
        String userId,

        @Schema(description = "이름", example = "김민준")
        String name,

        @Schema(description = "전역 권한. 서열형 ADMIN > MASTER > MEMBER", example = "MEMBER")
        String role,

        @Schema(description = "NORMAL · RESET_REQUIRED. RESET_REQUIRED 면 변경 전까지 다른 기능 사용 불가",
                example = "RESET_REQUIRED")
        String passwordStatus,

        @Schema(description = "부서명", example = "개발팀")
        String departmentName,

        @Schema(description = "부서 경로 (2단)", example = "기술본부 / 개발팀")
        String departmentPath,

        @Schema(description = "직급명", example = "대리")
        String jobPositionName
) {

    public static LoginResponse from(UserProfileRow row) {
        return new LoginResponse(
                row.userId(),
                row.name(),
                row.role(),
                row.passwordStatus(),
                row.departmentName(),
                row.departmentPath(),
                row.jobPositionName()
        );
    }
}
