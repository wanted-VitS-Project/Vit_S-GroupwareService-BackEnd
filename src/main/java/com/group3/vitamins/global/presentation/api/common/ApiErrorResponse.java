package com.group3.vitamins.global.presentation.api.common;

import com.group3.vitamins.global.domain.common.error.ErrorCode;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
    public static ApiErrorResponse of(ErrorCode errorCode, String path) {
        return new ApiErrorResponse(
                Instant.now(),
                errorCode.getStatus(),
                errorCode.getCode(),
                errorCode.getMessage(),
                path
        );
    }

    public static ApiErrorResponse of(int status, String code, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, code, message, path);
    }
}