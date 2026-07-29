package com.group3.vitamins.global.presentation.api.common;

import com.group3.vitamins.global.domain.common.error.ErrorCode;

import java.time.Instant;

public record ApiResponse<T>(
        Instant timestamp,
        int status,
        String code,
        String message,
        T data
) {
    public static <T> ApiResponse<T> success(T data) {
        return of(ErrorCode.COMMON_SUCCESS, data);
    }

    public static ApiResponse<Void> success() {
        return of(ErrorCode.COMMON_SUCCESS, null);
    }

    public static <T> ApiResponse<T> of(ErrorCode code, T data) {
        return new ApiResponse<>(
                Instant.now(),
                code.getStatus(),
                code.getCode(),
                code.getMessage(),
                data
        );
    }
}