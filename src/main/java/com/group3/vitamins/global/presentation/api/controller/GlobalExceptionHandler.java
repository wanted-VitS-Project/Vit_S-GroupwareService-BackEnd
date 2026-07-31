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
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // TODO: 도메인별 ErrorCode가 준비되기 전까지 Spring framework 예외에만 임시로 사용하는 공통 코드입니다.
    private static final String COMMON_BAD_REQUEST_CODE = "COMMON-BAD-REQUEST";
    private static final String COMMON_BAD_REQUEST_MESSAGE = "잘못된 요청입니다.";
    private static final String COMMON_VALIDATION_FAILED_CODE = "COMMON-VALIDATION-FAILED";
    private static final String COMMON_VALIDATION_FAILED_MESSAGE = "요청 값 검증에 실패했습니다.";
    private static final String COMMON_UNAUTHORIZED_CODE = "COMMON-UNAUTHORIZED";
    private static final String COMMON_UNAUTHORIZED_MESSAGE = "인증이 필요합니다.";
    private static final String COMMON_FORBIDDEN_CODE = "COMMON-FORBIDDEN";
    private static final String COMMON_FORBIDDEN_MESSAGE = "접근 권한이 없습니다.";
    private static final String INTERNAL_ERROR_CODE = "INTERNAL-ERROR";
    private static final String INTERNAL_ERROR_MESSAGE = "서버 내부 오류가 발생했습니다.";

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(
            DomainException e,
            HttpServletRequest request
    ) {
        log.warn("[{}] {} - path: {}", e.getHttpStatus(), e.getMessage(), request.getRequestURI());

        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiErrorResponse.of(e.getHttpStatus(), e.getErrorCode(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(COMMON_VALIDATION_FAILED_MESSAGE);

        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                400,
                COMMON_VALIDATION_FAILED_CODE,
                message,
                request.getRequestURI()
        ));
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

        if (!hasText(message)) {
            message = COMMON_VALIDATION_FAILED_MESSAGE;
        }

        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                400,
                COMMON_VALIDATION_FAILED_CODE,
                message,
                request.getRequestURI()
        ));
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
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                400,
                COMMON_BAD_REQUEST_CODE,
                COMMON_BAD_REQUEST_MESSAGE,
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException e,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(401).body(ApiErrorResponse.of(
                401,
                COMMON_UNAUTHORIZED_CODE,
                COMMON_UNAUTHORIZED_MESSAGE,
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(403).body(ApiErrorResponse.of(
                403,
                COMMON_FORBIDDEN_CODE,
                COMMON_FORBIDDEN_MESSAGE,
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("[500] unexpected exception - path: {}", request.getRequestURI(), e);

        return ResponseEntity.internalServerError().body(ApiErrorResponse.of(
                500,
                INTERNAL_ERROR_CODE,
                INTERNAL_ERROR_MESSAGE,
                request.getRequestURI()
        ));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
