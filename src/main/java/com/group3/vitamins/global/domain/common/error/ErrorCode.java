package com.group3.vitamins.global.domain.common.error;

public enum ErrorCode {

    COMMON_SUCCESS(200, "COMMON-SUCCESS", "요청이 성공적으로 처리되었습니다."),
    COMMON_BAD_REQUEST(400, "COMMON-BAD-REQUEST", "잘못된 요청입니다."),
    COMMON_VALIDATION_FAILED(400, "COMMON-VALIDATION-FAILED", "요청 값 검증에 실패했습니다."),
    COMMON_UNAUTHORIZED(401, "COMMON-UNAUTHORIZED", "인증이 필요합니다."),
    COMMON_FORBIDDEN(403, "COMMON-FORBIDDEN", "접근 권한이 없습니다."),
    COMMON_NOT_FOUND(404, "COMMON-NOT-FOUND", "요청한 리소스를 찾을 수 없습니다."),
    COMMON_CONFLICT(409, "COMMON-CONFLICT", "이미 존재하거나 충돌하는 데이터입니다."),
    EXTERNAL_SERVICE_ERROR(502, "EXTERNAL-SERVICE-ERROR", "외부 서비스 호출에 실패했습니다."),
    INTERNAL_ERROR(500, "INTERNAL-ERROR", "서버 내부 오류가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}