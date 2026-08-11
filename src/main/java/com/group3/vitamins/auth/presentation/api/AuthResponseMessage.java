package com.group3.vitamins.auth.presentation.api;

/**
 * 인증 API 성공 응답 메시지 상수 (`.ai/api/auth.md`). 문자열은 프론트 계약이라 바꾸지 않는다.
 */
public final class AuthResponseMessage {

    private AuthResponseMessage() {
    }

    public static final String LOGIN_SUCCESS = "로그인 성공";
    public static final String LOGOUT_SUCCESS = "로그아웃 성공";
    public static final String MY_INFO_SUCCESS = "조회 성공";
    public static final String TERMS_AGREED = "약관에 동의했습니다.";
    public static final String PASSWORD_CHANGED = "비밀번호가 변경되었습니다.";
}
