package com.group3.vitamins.global.presentation.api.common;

import com.group3.vitamins.global.domain.common.error.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 전역 예외 → 명세 형식의 실패 응답 변환.
 *
 * <pre>
 *   { "httpStatus": 401, "message": "...", "code": "AUTH_UNAUTHENTICATED" }
 * </pre>
 *
 * <p>Spring Security 필터 단계의 401·403 도 {@code CustomAuthenticationEntryPoint} ·
 * {@code CustomAccessDeniedHandler} 가 {@code HandlerExceptionResolver} 로 넘겨 여기로 온다.
 * 덕분에 <b>응답 형식과 에러 코드가 이 파일 하나에서만 정의된다.</b>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 401 은 전 도메인이 같은 코드를 쓴다 (.ai/api/ 전 파일 공통)
    private static final String AUTH_UNAUTHENTICATED = "AUTH_UNAUTHENTICATED";
    private static final String AUTH_UNAUTHENTICATED_MESSAGE = "로그인이 필요합니다.";

    // 아래 5개는 명세에 대응 코드가 없는 프레임워크 레벨 오류의 폴백이다.
    // 도메인 에러는 반드시 명세의 코드(ACC_NOT_FOUND · EMP_INVALID_REQUEST …)를 쓴다.
    private static final String COMMON_FORBIDDEN = "COMMON_FORBIDDEN";
    private static final String COMMON_FORBIDDEN_MESSAGE = "접근 권한이 없습니다.";
    private static final String COMMON_INVALID_REQUEST = "COMMON_INVALID_REQUEST";
    private static final String COMMON_INVALID_REQUEST_MESSAGE = "잘못된 요청입니다.";
    private static final String COMMON_NOT_FOUND = "COMMON_NOT_FOUND";
    private static final String COMMON_NOT_FOUND_MESSAGE = "요청한 경로를 찾을 수 없습니다.";
    private static final String COMMON_METHOD_NOT_ALLOWED = "COMMON_METHOD_NOT_ALLOWED";
    private static final String COMMON_METHOD_NOT_ALLOWED_MESSAGE = "지원하지 않는 요청 방식입니다.";
    private static final String COMMON_INTERNAL_ERROR = "COMMON_INTERNAL_ERROR";
    private static final String COMMON_INTERNAL_ERROR_MESSAGE = "서버 내부 오류가 발생했습니다.";

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(
            DomainException e,
            HttpServletRequest request
    ) {
        log.warn("[{}] {} {} - {} : {}",
                e.getHttpStatus(), request.getMethod(), request.getRequestURI(),
                e.getErrorCode().getCode(), e.getMessage());

        // e.getMessage() 를 쓴다 — 잠금 해제 시각처럼 상황별 메시지를 담는 경우가 있다 (auth.md)
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiErrorResponse.of(e.getHttpStatus(), e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(COMMON_INVALID_REQUEST_MESSAGE);

        return badRequest(message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .filter(this::hasText)
                .collect(Collectors.joining(", "));

        return badRequest(hasText(message) ? message : COMMON_INVALID_REQUEST_MESSAGE, request);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            ServletRequestBindingException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequestException(
            Exception e,
            HttpServletRequest request
    ) {
        return badRequest(COMMON_INVALID_REQUEST_MESSAGE, request);
    }

    /** 미인증 — Security 필터에서 넘어온다 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException e,
            HttpServletRequest request
    ) {
        log.debug("[401] {} {} - {}", request.getMethod(), request.getRequestURI(), e.getMessage());

        return ResponseEntity.status(401).body(ApiErrorResponse.of(
                401, AUTH_UNAUTHENTICATED, AUTH_UNAUTHENTICATED_MESSAGE));
    }

    /** 인증은 됐으나 권한 부족 — 도메인별 403 은 DomainException 으로 처리된다 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        log.warn("[403] {} {}", request.getMethod(), request.getRequestURI());

        return ResponseEntity.status(403).body(ApiErrorResponse.of(
                403, COMMON_FORBIDDEN, COMMON_FORBIDDEN_MESSAGE));
    }

    /**
     * 존재하지 않는 경로 — {@code 404}.
     *
     * <p>🚨 <b>이 핸들러가 없으면 아래 {@code Exception} 폴백이 삼켜 500 이 나간다.</b>
     * 프론트가 "URL 오타" 와 "서버 장애" 를 구분하지 못하고, 정상 상황인데 {@code ERROR} 로그가 쌓인다.
     *
     * <p>Spring Boot 3 은 매핑 없는 요청을 정적 리소스로 넘겨 {@link NoResourceFoundException} 을 던진다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException e,
            HttpServletRequest request
    ) {
        log.warn("[404] {} {}", request.getMethod(), request.getRequestURI());

        return ResponseEntity.status(404).body(ApiErrorResponse.of(
                404, COMMON_NOT_FOUND, COMMON_NOT_FOUND_MESSAGE));
    }

    /**
     * 경로는 맞지만 HTTP 메서드가 없는 경우 — {@code 405}.
     *
     * <p>🚨 <b>{@link NoResourceFoundException} 과 같은 이유로 필요하다.</b> 이 핸들러가 없으면 아래
     * {@code Exception} 폴백이 삼켜 <b>500 이 나가고</b>, 프론트가 "메서드를 잘못 썼다"(프론트 버그)와
     * "서버가 터졌다"(장애)를 구분하지 못한다. 실제로 {@code PATCH /api/v1/notifications} 처럼
     * GET 만 있는 경로에 다른 메서드를 쓰면 500 이 나가고 있었다 (2026-08-07 발견).
     *
     * <p>{@code Allow} 헤더에 지원 메서드를 담아 준다 — RFC 9110 이 405 응답에 요구하는 헤더다.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {
        log.warn("[405] {} {} - 지원 메서드: {}",
                request.getMethod(), request.getRequestURI(), e.getSupportedHttpMethods());

        ResponseEntity.BodyBuilder response = ResponseEntity.status(405);
        if (e.getSupportedHttpMethods() != null) {
            response.allow(e.getSupportedHttpMethods().toArray(new HttpMethod[0]));
        }
        return response.body(ApiErrorResponse.of(
                405, COMMON_METHOD_NOT_ALLOWED, COMMON_METHOD_NOT_ALLOWED_MESSAGE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("[500] {} {}", request.getMethod(), request.getRequestURI(), e);

        return ResponseEntity.status(500).body(ApiErrorResponse.of(
                500, COMMON_INTERNAL_ERROR, COMMON_INTERNAL_ERROR_MESSAGE));
    }

    private ResponseEntity<ApiErrorResponse> badRequest(String message, HttpServletRequest request) {
        log.warn("[400] {} {} - {}", request.getMethod(), request.getRequestURI(), message);
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, COMMON_INVALID_REQUEST, message));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
