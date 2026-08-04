package com.group3.vitamins.text.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TextErrorCode implements ErrorCode {

    //TXT- 쭉 작성
    FORBIDDEN("TXT-001", "편집 권한이 없습니다."),
    BLOCK_NOT_FOUND("TXT-002", "존재하지 않는 블록입니다."),
    INVALID_CONTENT("TXT-003", "내용을 입력해 주세요.");
    // 401(미인증)·403(RESET_REQUIRED)은 여기 도메인 코드로 안 만든다 — 전 도메인 공통으로
    // AUTH_UNAUTHENTICATED/AUTH_PASSWORD_RESET_REQUIRED 를 쓴다 (GlobalExceptionHandler·PasswordResetGateFilter).
    // 500(예상 못한 서버 오류)도 도메인 코드를 안 만든다 — 이런 오류는 애초에 "이 조건이면 TXT-00X를 던진다"는
    // 코드가 없어서 항상 GlobalExceptionHandler 의 범용 Exception 핸들러가 COMMON_INTERNAL_ERROR 로 받는다.

    private final String code;
    private final String message;
}
