package com.group3.vitamins.global.domain.common.error.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

public class ConflictException extends DomainException {
    public ConflictException() {
        super(ErrorCode.COMMON_CONFLICT);
    }

    public ConflictException(String message) {
        super(ErrorCode.COMMON_CONFLICT, message);
    }
}