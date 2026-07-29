package com.group3.vitamins.global.domain.common.error.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

public class ValidationException extends DomainException {

    public ValidationException() {
        super(ErrorCode.COMMON_BAD_REQUEST);
    }

    public ValidationException(String message) {
        super(ErrorCode.COMMON_BAD_REQUEST, message);
    }
}