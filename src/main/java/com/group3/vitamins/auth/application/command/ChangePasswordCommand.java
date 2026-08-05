package com.group3.vitamins.auth.application.command;

/**
 * 비밀번호 변경 커맨드 (`.ai/api/auth.md` §4).
 *
 * <p>{@code currentPassword} 는 최초 변경(`RESET_REQUIRED`)일 때만 생략할 수 있다 —
 * 이미 임시 비밀번호로 인증해 세션이 있기 때문이다.
 *
 * @param userId              세션에서 온 본인 사번
 * @param currentPassword     현재 비밀번호 (최초 변경이면 {@code null} 가능)
 * @param newPassword         새 비밀번호
 * @param newPasswordConfirm  새 비밀번호 확인
 */
public record ChangePasswordCommand(
        String userId,
        String currentPassword,
        String newPassword,
        String newPasswordConfirm
) {
}
