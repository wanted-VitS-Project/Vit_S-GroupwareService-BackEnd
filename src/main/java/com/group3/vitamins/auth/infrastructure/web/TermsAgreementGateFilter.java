package com.group3.vitamins.auth.infrastructure.web;

import com.group3.vitamins.auth.domain.exception.AuthErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Set;

/**
 * 최초 로그인 시 <b>약관에 동의하기 전에는 다른 기능을 못 쓰게</b> 막는다 (`auth.md` §5 · §6-7).
 *
 * <p>{@link PasswordResetGateFilter} 와 같은 방식(세션 속성)이며, <b>약관 게이트가 비밀번호 게이트보다 앞</b>이다 —
 * 순서상 약관 동의 → 비밀번호 변경. 그래서 예외 경로에 {@code /auth/password} 를 넣지 않는다(약관 전엔 비번 변경도 막는다).
 *
 * <p>프론트가 화면만 가리는 것으로는 부족하다 — API 를 직접 호출하면 그대로 뚫린다.
 */
@RequiredArgsConstructor
public class TermsAgreementGateFilter extends OncePerRequestFilter {

    /** 약관에 동의하기 위해 반드시 열려 있어야 하는 경로 */
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/v1/auth/terms-agreements",   // 동의하러 가는 곳
            "/api/v1/auth/logout",             // 안 하고 나갈 자유
            "/api/v1/auth/me"                  // 상태를 확인해야 화면을 그린다
    );

    private final HandlerExceptionResolver resolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isBlocked(request)) {
            // 응답 형식·에러 코드를 GlobalExceptionHandler 한 곳에서만 만들기 위해 위임한다
            resolver.resolveException(request, response, null,
                    new ForbiddenException(AuthErrorCode.AUTH_TERMS_AGREEMENT_REQUIRED));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlocked(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;   // 미인증 요청은 Security 가 판단한다
        }
        if (!Boolean.TRUE.equals(session.getAttribute(AuthSessionManager.TERMS_AGREEMENT_REQUIRED))) {
            return false;
        }
        // CORS preflight 는 통과시킨다 — 막으면 브라우저가 본 요청을 보내지도 않는다
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return false;
        }
        // getRequestURI() 는 컨텍스트 경로를 포함한다. 컨텍스트 경로를 떼고 비교해야
        // server.servlet.context-path 설정·프록시 배포 시에도 예외 경로가 어긋나지 않는다 (미설정이면 "" 라 무해).
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !ALLOWED_PATHS.contains(path);
    }
}
