package com.group3.vitamins.auth.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AUTH 도메인 에러 코드.
 *
 * <p>⛔ <b>여기 있는 코드와 메시지는 프론트와의 계약이다</b> (`.ai/api/auth.md` · 노션 확정).
 * 임의로 추가·변경하지 마라. 필요하면 노션을 먼저 고친다 (`.ai/API.md` §0).
 *
 * <p>{@code AUTH_HASHING_BUSY}(503) 는 여기 없다 — 도메인 로직이 아니라 해시 인프라가 던지므로
 * {@code global.domain.common.error.exception.PasswordHashingBusyException} 이 갖고 있다.
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    // --- 로그인 ---
    AUTH_INVALID_REQUEST("AUTH_INVALID_REQUEST", "필수 입력값이 누락되었습니다."),
    /** ⚠️ 사번 존재 여부를 구분하지 않는다 (AUTH-003). 없는 사번·틀린 비밀번호 모두 이 코드다 */
    AUTH_LOGIN_FAILED("AUTH_LOGIN_FAILED", "사번 또는 비밀번호가 올바르지 않습니다."),
    AUTH_ACCOUNT_INACTIVE("AUTH_ACCOUNT_INACTIVE", "비활성화된 계정입니다. 관리자에게 문의하세요."),
    AUTH_ACCOUNT_LOCKED("AUTH_ACCOUNT_LOCKED", "로그인 실패가 누적되어 계정이 잠겼습니다."),
    AUTH_TOO_MANY_REQUESTS("AUTH_TOO_MANY_REQUESTS", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),

    // --- 세션 ---
    AUTH_UNAUTHENTICATED("AUTH_UNAUTHENTICATED", "로그인이 필요합니다."),
    /**
     * ⭐ 명세에 없어 <b>내가 추가한 코드</b> — 노션 반영 시 팀 합의 필요.
     * 명세는 "RESET_REQUIRED 면 변경 전까지 다른 기능 사용 불가"(ACC-006)만 정의하고
     * 위반 시 응답을 정하지 않았다. 프론트가 "비밀번호 변경 화면으로 보내기"로 분기하려면 코드가 필요하다.
     */
    AUTH_PASSWORD_RESET_REQUIRED("AUTH_PASSWORD_RESET_REQUIRED", "초기 비밀번호를 먼저 변경해 주세요."),

    // --- 비밀번호 변경 ---
    AUTH_CURRENT_PASSWORD_REQUIRED("AUTH_CURRENT_PASSWORD_REQUIRED", "현재 비밀번호를 입력해 주세요."),
    AUTH_CURRENT_PASSWORD_INVALID("AUTH_CURRENT_PASSWORD_INVALID", "현재 비밀번호가 올바르지 않습니다."),
    AUTH_PASSWORD_CONFIRM_MISMATCH("AUTH_PASSWORD_CONFIRM_MISMATCH", "새 비밀번호와 확인이 일치하지 않습니다."),
    AUTH_PASSWORD_POLICY_VIOLATION("AUTH_PASSWORD_POLICY_VIOLATION",
            "비밀번호는 8자 이상이며 영문·숫자·특수문자를 모두 포함해야 합니다."),
    AUTH_PASSWORD_UNCHANGED("AUTH_PASSWORD_UNCHANGED", "새 비밀번호가 현재 비밀번호와 같습니다.");

    private final String code;
    private final String message;
}
