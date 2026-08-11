package com.group3.vitamins.global.domain.common.error.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

/**
 * 레이트리밋 초과 (429).
 *
 * <p>요청 내용에는 문제가 없고 <b>빈도</b>가 문제다. 400 대의 다른 코드와 구분해야
 * 프론트가 "잠시 후 재시도" 로 안내할 수 있다.
 */
public class TooManyRequestsException extends DomainException {

    public TooManyRequestsException(ErrorCode errorCode) {
        super(errorCode);
    }

    public TooManyRequestsException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /** 상황별 메시지가 필요한 경우 (코드는 유지) */
    public TooManyRequestsException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    @Override
    public int getHttpStatus() {
        return 429;
    }
}
