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
    INTERNAL_ERROR("CHK-004", "서버 내부 오류입니다."),
    INVALID_CONTENT("CHK-005", "내용을 입력해 주세요."),
    NO_FIELD_TO_UPDATE("CHK-006", "수정할 내용을 하나 이상 입력해 주세요.");
    // 401(미인증)·403(RESET_REQUIRED)은 여기 도메인 코드로 안 만든다 — 전 도메인 공통으로
    // AUTH_UNAUTHENTICATED/AUTH_PASSWORD_RESET_REQUIRED 를 쓴다 (GlobalExceptionHandler·PasswordResetGateFilter).

    private final String code;
    private final String message;
}
