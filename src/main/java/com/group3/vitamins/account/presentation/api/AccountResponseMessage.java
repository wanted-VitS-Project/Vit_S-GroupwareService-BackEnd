package com.group3.vitamins.account.presentation.api;

/**
 * 계정 API 성공 응답 메시지 상수 (`.ai/api/account.md`).
 */
public final class AccountResponseMessage {

    private AccountResponseMessage() {
    }

    public static final String ROLE_CHANGED = "권한이 변경되었습니다.";
    public static final String STATUS_CHANGED = "계정 상태가 변경되었습니다.";
    public static final String PASSWORD_RESET = "비밀번호 재설정 완료";
}
