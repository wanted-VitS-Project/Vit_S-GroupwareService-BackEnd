package com.group3.vitamins.activitylog.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityLogErrorCode implements ErrorCode {

    ACTIVITY_LOG_CURSOR_INVALID("ACTIVITY_LOG_CURSOR_INVALID", "잘못된 커서입니다."),
    ACTIVITY_LOG_SIZE_INVALID("ACTIVITY_LOG_SIZE_INVALID", "잘못된 조회 개수입니다."),
    ACTIVITY_LOG_BLOCK_STEP_MISMATCH("ACTIVITY_LOG_BLOCK_STEP_MISMATCH", "Block이 요청한 Step에 속하지 않습니다."),
    BLOCK_NOT_FOUND("BLOCK_NOT_FOUND", "Block이 존재하지 않습니다.");

    private final String code;
    private final String message;
}