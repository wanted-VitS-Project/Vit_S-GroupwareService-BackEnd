package com.group3.vitamins.global.presentation.api.controller;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;
import com.group3.vitamins.global.presentation.api.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(
            DomainException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[{}] {} - path: {}", errorCode.getStatus(), e.getMessage(), request.getRequestURI());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiErrorResponse.of(
                        errorCode.getStatus(),
                        errorCode.getCode(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(ErrorCode.COMMON_VALIDATION_FAILED.getMessage());

        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                400,
                ErrorCode.COMMON_VALIDATION_FAILED.getCode(),
                message,
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                400,
                ErrorCode.COMMON_VALIDATION_FAILED.getCode(),
                e.getMessage(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException e,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(401).body(ApiErrorResponse.of(
                ErrorCode.COMMON_UNAUTHORIZED,
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(403).body(ApiErrorResponse.of(
                ErrorCode.COMMON_FORBIDDEN,
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
                ErrorCode.INTERNAL_ERROR,
                request.getRequestURI()
        ));
    }
}