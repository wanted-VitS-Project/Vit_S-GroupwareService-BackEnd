package com.group3.vitamins.global.presentation.api.common;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 잘못된 HTTP 메서드 처리(405).
 *
 * <p>이 핸들러가 없으면 {@code Exception} 폴백이 삼켜 <b>500</b> 이 나갔다 — 프론트가 자기 버그(메서드
 * 오용)와 서버 장애를 구분하지 못한다. 2026-08-07 에 실제로 그 상태였다.
 */
@DisplayName("GlobalExceptionHandler — 405 · 끊긴 비동기 응답")
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

    /**
     * ⚠️ {@code null} 반환이 계약이다. 응답 헤더가 {@code text/event-stream} 으로 굳은 뒤라
     * {@code ApiErrorResponse} 를 쓰려 하면 {@code HttpMessageNotWritableException} 이 한 번 더 난다.
     */
    @Test
    @DisplayName("스트림이 끊긴 뒤(비동기 디스패치) I/O 오류는 500 이 아니라 아무것도 쓰지 않는다")
    void clientDisconnectDuringAsyncWritesNothing() {
        assertThat(handler.handleClientDisconnect(
                new IOException("소켓 끊김"), streamRequest(), streamResponse())).isNull();
    }

    /**
     * ⚠️ 비동기라는 이유만으로 삼키면, 앞으로 추가될 다른 비동기 엔드포인트의 파일·네트워크 오류까지
     * 조용히 사라져 <b>클라이언트가 빈 200 을 받는다</b>. 아직 응답을 쓰지 않았다면 알려줄 수 있으므로
     * 500 이어야 한다.
     */
    @Test
    @DisplayName("비동기라도 아직 응답을 쓰지 않았다면 진짜 I/O 오류는 500 으로 알려준다")
    void asyncFailureBeforeAnyWriteStill500() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/files/1");
        request.setDispatcherType(DispatcherType.ASYNC);

        // 커밋 전이고 SSE 도 아니다 — 에러 응답을 정상적으로 내려보낼 수 있는 상태
        ResponseEntity<ApiErrorResponse> response = handler.handleClientDisconnect(
                new IOException("디스크 오류"), request, new MockHttpServletResponse());

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("COMMON_INTERNAL_ERROR");
    }

    @Test
    @DisplayName("비동기 + 이미 커밋된 응답이면 상태코드를 못 바꾸므로 조용히 넘긴다")
    void asyncFailureAfterCommitWritesNothing() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/files/1");
        request.setDispatcherType(DispatcherType.ASYNC);

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCommitted(true);

        assertThat(handler.handleClientDisconnect(new IOException("디스크 오류"), request, response))
                .isNull();
    }

    /**
     * ⚠️ 로케일 회귀 방지. Spring 의 {@code DisconnectedClientHelper} 는 메시지가 {@code "broken pipe"}
     * 인지를 본다 — 한글 윈도우에서는 그 문자열이 아니어서 판별에 실패했다(2026-08-14 로컬 재현).
     * 디스패치 타입으로 거르므로 <b>메시지가 무엇이든</b> 통과해야 한다.
     */
    @Test
    @DisplayName("OS 로케일이 영어가 아니어도(메시지가 한글이어도) 똑같이 조용히 넘긴다")
    void clientDisconnectIsDetectedRegardlessOfLocale() {
        IOException windowsKorean = new IOException(
                "현재 연결은 사용자의 호스트 시스템의 소프트웨어의 의해 중단되었습니다");

        assertThat(handler.handleClientDisconnect(windowsKorean, streamRequest(), streamResponse()))
                .isNull();
    }

    @Test
    @DisplayName("영문 메시지(broken pipe)는 일반 요청에서도 조용히 넘긴다")
    void brokenPipeIsNotAnError() {
        assertThat(handler.handleClientDisconnect(
                new IOException("Broken pipe"), request(), new MockHttpServletResponse()))
                .isNull();
    }

    @Test
    @DisplayName("일반 요청의 진짜 I/O 오류는 기존대로 500 을 내려준다")
    void realIoFailureStill500() {
        ResponseEntity<ApiErrorResponse> response = handler.handleClientDisconnect(
                new IOException("디스크 오류"), request(), new MockHttpServletResponse());

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("COMMON_INTERNAL_ERROR");
    }

    /** 알림 SSE 구독 중 오류가 난 상황 — 비동기 디스패치 + `text/event-stream` 응답 */
    private MockHttpServletRequest streamRequest() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/notifications/stream");
        request.setDispatcherType(DispatcherType.ASYNC);
        return request;
    }

    private MockHttpServletResponse streamResponse() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        return response;
    }
}
