package com.group3.vitamins.global.domain.common.error.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

public class ForbiddenException extends DomainException {

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ForbiddenException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /** 잠금 해제 시각처럼 상황별 메시지가 필요한 경우 (코드는 유지) */
    public ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    @Override
    public int getHttpStatus() {
        return 403;
    }
}
