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

@DisplayName("TermsAgreementGateFilter 약관 게이트")
class TermsAgreementGateFilterTest {

    private HandlerExceptionResolver resolver;
    private FilterChain chain;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private TermsAgreementGateFilter filter;

    @BeforeEach
    void setUp() {
        resolver = Mockito.mock(HandlerExceptionResolver.class);
        chain = Mockito.mock(FilterChain.class);
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        session = Mockito.mock(HttpSession.class);
        filter = new TermsAgreementGateFilter(resolver);
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("GET");
    }

    private void givenTermsRequired(boolean required) {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthSessionManager.TERMS_AGREEMENT_REQUIRED)).thenReturn(required);
    }

    @Test
    @DisplayName("약관 미동의 상태에서 일반 경로(비밀번호 변경 포함)는 403 AUTH_TERMS_AGREEMENT_REQUIRED 로 막힌다")
    void blocksOtherPathsWhenTermsRequired() throws Exception {
        givenTermsRequired(true);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/password");   // 약관 전엔 비번 변경도 막힌다

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(eq(request), eq(response), isNull(), captor.capture());
        assertThat(((DomainException) captor.getValue()).getErrorCode())
                .isEqualTo(AuthErrorCode.AUTH_TERMS_AGREEMENT_REQUIRED);
    }

    @Test
    @DisplayName("약관 동의 엔드포인트·로그아웃·내정보는 미동의 상태에서도 통과한다")
    void allowsExemptPaths() throws Exception {
        for (String path : new String[]{
                "/api/v1/auth/terms-agreements", "/api/v1/auth/logout", "/api/v1/auth/me"}) {
            Mockito.reset(chain, resolver);
            givenTermsRequired(true);
            when(request.getRequestURI()).thenReturn(path);

            filter.doFilterInternal(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
            verify(resolver, never()).resolveException(any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("이미 동의했으면(플래그 false) 어떤 경로든 통과한다")
    void passesWhenNotRequired() throws Exception {
        givenTermsRequired(false);
        when(request.getRequestURI()).thenReturn("/api/v1/departments");

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("세션이 없으면(미인증) 통과시켜 Security 가 판단하게 한다")
    void passesWhenNoSession() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("CORS preflight(OPTIONS)는 통과시킨다")
    void passesPreflight() throws Exception {
        givenTermsRequired(true);
        when(request.getMethod()).thenReturn("OPTIONS");
        when(request.getRequestURI()).thenReturn("/api/v1/departments");

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("컨텍스트 경로가 있어도 예외 경로가 어긋나지 않는다")
    void stripsContextPath() throws Exception {
        givenTermsRequired(true);
        when(request.getContextPath()).thenReturn("/vitamins");
        when(request.getRequestURI()).thenReturn("/vitamins/api/v1/auth/terms-agreements");

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);   // 컨텍스트 경로 제거 후 허용 경로와 일치
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }
}
