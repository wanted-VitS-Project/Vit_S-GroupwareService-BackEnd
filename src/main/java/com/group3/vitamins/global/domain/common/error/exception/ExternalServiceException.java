package com.group3.vitamins.global.domain.common.error.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

public class ExternalServiceException extends DomainException {
    public ExternalServiceException() {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    public ExternalServiceException(String message) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, message);
    }

    public ExternalServiceException(Throwable cause) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, cause);
    }
}
