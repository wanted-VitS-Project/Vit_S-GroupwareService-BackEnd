package com.group3.vitamins.global.presentation.api.common;

import com.group3.vitamins.global.domain.common.error.ErrorCode;

/**
 * 실패 응답 래퍼.
 *
 * <pre>
 *   { "httpStatus": 401, "message": "사번 또는 비밀번호가 올바르지 않습니다.", "code": "AUTH_LOGIN_FAILED" }
 * </pre>
 *
 * <p>⚠️ 성공 응답과 달리 {@code data} 가 없고 {@code code} 가 있다. 명세가 정한 형태다 (`.ai/api/`).
 * <p>{@code timestamp}·{@code path} 는 명세에 없어 제거했다. 두 값은 서버 로그에 남는다.
 *
 * <p>💡 <b>에러 응답에 {@code data} 가 없다는 점이 설계에 영향을 준다.</b> 예를 들어 엑셀 일괄 등록의
 * 검증 실패 목록은 여기 담을 수 없어 {@code 200 + data.registered:false + data.errors[]} 로 내려간다.
 */
public record ApiErrorResponse(
        int httpStatus,
        String message,
        String code
) {

    public static ApiErrorResponse of(int httpStatus, ErrorCode errorCode) {
        return new ApiErrorResponse(httpStatus, errorCode.getMessage(), errorCode.getCode());
    }

    /** 잠금 해제 시각처럼 <b>메시지에 상황별 정보를 담아야</b> 하는 경우 (`auth.md` — `AUTH_ACCOUNT_LOCKED`) */
    public static ApiErrorResponse of(int httpStatus, ErrorCode errorCode, String message) {
        return new ApiErrorResponse(httpStatus, message, errorCode.getCode());
    }

    public static ApiErrorResponse of(int httpStatus, String code, String message) {
        return new ApiErrorResponse(httpStatus, message, code);
    }
}
