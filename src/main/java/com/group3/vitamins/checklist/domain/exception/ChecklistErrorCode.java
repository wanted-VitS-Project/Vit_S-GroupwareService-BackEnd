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
    UNAUTHORIZED("CHK-004", "다시 로그인해주세요."),
    INTERNAL_ERROR("CHK-005", "서버 내부 오류입니다.");

    private final String code;
    private final String message;
}
