package com.group3.vitamins.global.domain.common.error.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

public class ConflictException extends DomainException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConflictException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    @Override
    public int getHttpStatus() {
        return 409;
    }
}
