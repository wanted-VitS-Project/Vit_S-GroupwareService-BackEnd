package com.group3.vitamins.global.presentation.api.common;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 잘못된 HTTP 메서드 처리(405).
 *
 * <p>이 핸들러가 없으면 {@code Exception} 폴백이 삼켜 <b>500</b> 이 나갔다 — 프론트가 자기 버그(메서드
 * 오용)와 서버 장애를 구분하지 못한다. 2026-08-07 에 실제로 그 상태였다.
 */
@DisplayName("GlobalExceptionHandler — 405")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest request() {
        return new MockHttpServletRequest("PATCH", "/api/v1/notifications");
    }

    @Test
    @DisplayName("지원하지 않는 메서드는 405 + COMMON_METHOD_NOT_ALLOWED 로 응답한다")
    void returnsMethodNotAllowed() {
        HttpRequestMethodNotSupportedException e =
                new HttpRequestMethodNotSupportedException("PATCH", List.of("GET"));

        ResponseEntity<ApiErrorResponse> response = handler.handleMethodNotSupported(e, request());

        assertThat(response.getStatusCode().value()).isEqualTo(405);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("COMMON_METHOD_NOT_ALLOWED");
        assertThat(response.getBody().httpStatus()).isEqualTo(405);
    }

    @Test
    @DisplayName("Allow 헤더에 지원 메서드를 담는다 (RFC 9110 이 405 응답에 요구)")
    void includesAllowHeader() {
        HttpRequestMethodNotSupportedException e =
                new HttpRequestMethodNotSupportedException("PATCH", List.of("GET", "POST"));

        ResponseEntity<ApiErrorResponse> response = handler.handleMethodNotSupported(e, request());

        assertThat(response.getHeaders().get(HttpHeaders.ALLOW))
                .isNotNull();
        assertThat(response.getHeaders().getAllow())
                .containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.POST);
    }

    @Test
    @DisplayName("지원 메서드 정보가 없어도 Allow 헤더는 남긴다 — 없는 메서드를 지어내지 않고 빈 값으로")
    void alwaysIncludesAllowHeader() {
        HttpRequestMethodNotSupportedException e = new HttpRequestMethodNotSupportedException("PATCH");

        ResponseEntity<ApiErrorResponse> response = handler.handleMethodNotSupported(e, request());

        assertThat(response.getStatusCode().value()).isEqualTo(405);
        assertThat(response.getHeaders().containsKey(HttpHeaders.ALLOW)).isTrue();
        assertThat(response.getHeaders().getAllow()).isEmpty();
    }

    @Test
    @DisplayName("비동기 요청 연결이 끊긴 경우 응답 바디를 다시 쓰려 하지 않고 조용히 끝난다")
    void ignoresAsyncRequestNotUsable() {
        AsyncRequestNotUsableException e =
                new AsyncRequestNotUsableException("client disconnected");

        assertThatCode(() -> handler.handleAsyncRequestNotUsable(e, request()))
                .doesNotThrowAnyException();
    }
}
