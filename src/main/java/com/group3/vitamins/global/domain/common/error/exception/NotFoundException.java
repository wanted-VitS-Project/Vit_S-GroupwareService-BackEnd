package com.group3.vitamins.global.domain.common.error.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

public class NotFoundException extends DomainException {
    public NotFoundException() {
        super(ErrorCode.COMMON_NOT_FOUND);
    }

    public NotFoundException(String message) {
        super(ErrorCode.COMMON_NOT_FOUND, message);
    }
}