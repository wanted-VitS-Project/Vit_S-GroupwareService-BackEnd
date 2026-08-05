package com.group3.vitamins.auth.infrastructure.web;

import com.group3.vitamins.auth.application.AuthSessionManager;
import com.group3.vitamins.auth.domain.exception.AuthErrorCode;
import com.group3.vitamins.global.domain.common.error.DomainException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PasswordResetGateFilter 비밀번호 게이트")
class PasswordResetGateFilterTest {

    private HandlerExceptionResolver resolver;
    private FilterChain chain;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private PasswordResetGateFilter filter;

    @BeforeEach
    void setUp() {
        resolver = Mockito.mock(HandlerExceptionResolver.class);
        chain = Mockito.mock(FilterChain.class);
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        session = Mockito.mock(HttpSession.class);
        filter = new PasswordResetGateFilter(resolver);
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthSessionManager.PASSWORD_RESET_REQUIRED)).thenReturn(true);
    }

    @Test
    @DisplayName("변경 전 일반 경로는 403 AUTH_PASSWORD_RESET_REQUIRED 로 막힌다")
    void blocksOtherPaths() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/departments");

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(eq(request), eq(response), isNull(), captor.capture());
        assertThat(((DomainException) captor.getValue()).getErrorCode())
                .isEqualTo(AuthErrorCode.AUTH_PASSWORD_RESET_REQUIRED);
    }

    @Test
    @DisplayName("비밀번호 변경 경로는 통과한다")
    void allowsPasswordChange() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/password");

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("약관 동의 경로도 통과한다 — 최초 로그인은 약관·비번 게이트가 동시에 켜져 둘 다 통과해야 한다")
    void allowsTermsAgreement() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/terms-agreements");

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("이미 변경했으면(플래그 false) 어떤 경로든 통과한다")
    void passesWhenNotRequired() throws Exception {
        when(session.getAttribute(AuthSessionManager.PASSWORD_RESET_REQUIRED)).thenReturn(false);
        when(request.getRequestURI()).thenReturn("/api/v1/departments");

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }
}
