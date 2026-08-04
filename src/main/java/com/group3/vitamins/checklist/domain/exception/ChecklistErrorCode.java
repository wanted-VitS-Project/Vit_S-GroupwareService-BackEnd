package com.group3.vitamins.checklist.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChecklistErrorCode implements ErrorCode {

    //CHK- 쭉 작성
    FORBIDDEN("CHK-001", "편집 권한이 없습니다."),
    BLOCK_NOT_FOUND("CHK-002", "존재하지 않는 블록입니다."),
    ITEM_NOT_FOUND("CHK-003", "존재하지 않는 항목입니다."),
    INVALID_CONTENT("CHK-004", "내용을 입력해 주세요."),
    NO_FIELD_TO_UPDATE("CHK-005", "수정할 내용을 하나 이상 입력해 주세요.");
    // 401(미인증)·403(RESET_REQUIRED)은 여기 도메인 코드로 안 만든다 — 전 도메인 공통으로
    // AUTH_UNAUTHENTICATED/AUTH_PASSWORD_RESET_REQUIRED 를 쓴다 (GlobalExceptionHandler·PasswordResetGateFilter).
    // 500(예상 못한 서버 오류)도 도메인 코드를 안 만든다 — 이런 오류는 애초에 "이 조건이면 CHK-00X를 던진다"는
    // 코드가 없어서 항상 GlobalExceptionHandler 의 범용 Exception 핸들러가 COMMON_INTERNAL_ERROR 로 받는다.

    private final String code;
    private final String message;
}
