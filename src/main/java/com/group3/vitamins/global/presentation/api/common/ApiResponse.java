package com.group3.vitamins.global.presentation.api.common;

/**
 * 성공 응답 래퍼.
 *
 * <pre>
 *   { "httpStatus": 200, "message": "로그인 성공", "data": { ... } }
 * </pre>
 *
 * <p>⚠️ 형식은 노션 명세(`.ai/api/`)가 정한 <b>프론트와의 계약</b>이다.
 * 필드를 추가하거나 이름을 바꾸지 마라 (`.ai/API.md` §0).
 *
 * <p>{@code timestamp}·{@code code} 를 두지 않는 이유 — 명세에 없다. 디버깅 정보는 서버 로그에 남긴다.
 */
public record ApiResponse<T>(
        int httpStatus,
        String message,
        T data
) {

    public static <T> ApiResponse<T> of(int httpStatus, String message, T data) {
        return new ApiResponse<>(httpStatus, message, data);
    }

    /** 200 OK + 데이터 */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /** 200 OK + {@code data: null} — 로그아웃·비밀번호 변경처럼 반환값이 없는 경우 */
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(200, message, null);
    }

    /** 201 Created */
    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(201, message, data);
    }
}
