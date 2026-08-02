package com.group3.vitamins.global.presentation.api.common;

import java.time.Instant;

public record ApiResponse<T>(
        Instant timestamp,
        int status,
        String code,
        String message,
        T data
) {
    // TODO: 성공 응답 코드 정책이 확정되기 전까지 임시로 사용하는 공통 성공 코드입니다.
    private static final int SUCCESS_STATUS = 200;
    private static final String SUCCESS_CODE = "COMMON-SUCCESS";
    private static final String SUCCESS_MESSAGE = "요청이 성공적으로 처리되었습니다.";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                Instant.now(),
                SUCCESS_STATUS,
                SUCCESS_CODE,
                SUCCESS_MESSAGE,
                data
        );
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }
}
