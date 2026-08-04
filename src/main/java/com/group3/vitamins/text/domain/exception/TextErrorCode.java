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
    UNAUTHORIZED("TXT-003", "다시 로그인해주세요."),
    INTERNAL_ERROR("TXT-004", "서버 내부 오류입니다.");

    private final String code;
    private final String message;
}
