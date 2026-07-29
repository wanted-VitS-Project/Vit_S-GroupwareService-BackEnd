package com.group3.vitamins.global.domain.common.error.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

public class ForbiddenException extends DomainException {
    public ForbiddenException() {
        super(ErrorCode.COMMON_FORBIDDEN);
    }

    public ForbiddenException(String message) {
        super(ErrorCode.COMMON_FORBIDDEN, message);
    }
}