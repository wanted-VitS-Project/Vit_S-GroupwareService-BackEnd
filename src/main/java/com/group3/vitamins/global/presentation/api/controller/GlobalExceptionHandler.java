package com.group3.vitamins.global.presentation.api.controller;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.presentation.api.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    // 아래 3개는 명세에 대응 코드가 없는 프레임워크 레벨 오류의 폴백이다.
    // 도메인 에러는 반드시 명세의 코드(ACC_NOT_FOUND · EMP_INVALID_REQUEST …)를 쓴다.
    private static final String COMMON_FORBIDDEN = "COMMON_FORBIDDEN";
    private static final String COMMON_FORBIDDEN_MESSAGE = "접근 권한이 없습니다.";
    private static final String COMMON_INVALID_REQUEST = "COMMON_INVALID_REQUEST";
    private static final String COMMON_INVALID_REQUEST_MESSAGE = "잘못된 요청입니다.";
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
