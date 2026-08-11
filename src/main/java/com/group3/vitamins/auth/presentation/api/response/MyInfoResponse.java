package com.group3.vitamins.auth.presentation.api.response;

import com.group3.vitamins.auth.application.result.UserProfileRow;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

/** 마이페이지 화면이 쓰는 필드 전체 (`.ai/api/auth.md` §3) */
@Schema(description = "내 정보 조회 응답")
public record MyInfoResponse(

        @Schema(description = "사번", example = "EMP001")
        String userId,

        @Schema(description = "이름", example = "김민준")
        String name,

        @Schema(description = "전역 권한", example = "MEMBER")
        String role,

        @Schema(description = "AGREED · REQUIRED (ADMIN 은 항상 AGREED)", example = "AGREED")
        String termsStatus,

        @Schema(description = "NORMAL · RESET_REQUIRED", example = "NORMAL")
        String passwordStatus,

        @Schema(description = "이메일", example = "minjun@example.com")
        String email,

        @Schema(description = "연락처", example = "010-0000-0000")
        String phone,

        @Schema(description = "부서명", example = "개발팀")
        String departmentName,

        @Schema(description = "부서 경로 (2단)", example = "기술본부 / 개발팀")
        String departmentPath,

        @Schema(description = "직급명", example = "대리")
        String jobPositionName,

        @Schema(description = "입사일 yyyy-MM-dd", example = "2024-03-04")
        String hiredAt,

        @Schema(description = "마지막 로그인 yyyy-MM-dd HH:mm:ss", example = "2026-08-03 09:12:44")
        String lastLoginAt,

        @Schema(description = "프로필 사진 URL. 값은 /api/v1/employees/{userId}/profile-image, 사진이 없으면 null",
                example = "/api/v1/employees/vitas-EMP001/profile-image", nullable = true)
        String profileImageUrl
) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static MyInfoResponse from(UserProfileRow row) {
        return new MyInfoResponse(
                row.userId(),
                row.name(),
                row.role(),
                row.termsStatus(),
                row.passwordStatus(),
                row.email(),
                row.phone(),
                row.departmentName(),
                row.departmentPath(),
                row.jobPositionName(),
                row.hiredAt() == null ? null : row.hiredAt().format(DATE),
                row.lastLoginAt() == null ? null : row.lastLoginAt().format(DATE_TIME),
                row.profileImageUrl()
        );
    }
}
