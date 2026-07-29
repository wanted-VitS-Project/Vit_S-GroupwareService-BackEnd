package com.group3.vitamins.global.domain.common.error.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

public class UnauthorizedException extends DomainException {
    public UnauthorizedException() {
        super(ErrorCode.COMMON_UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        super(ErrorCode.COMMON_UNAUTHORIZED, message);
    }
}