package com.group3.vitamins.auth.infrastructure.web;

import com.group3.vitamins.auth.application.AuthSessionManager;
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
 * 초기 비밀번호를 바꾸기 전에는 다른 기능을 못 쓰게 막는다.
 *
 * <p>명세 요구사항이다 — {@code passwordStatus = RESET_REQUIRED} 면 <b>변경 전까지 다른 기능 사용 불가</b>
 * (`ACC-006` · `.ai/api/auth.md`).
 *
 * <p>프론트가 화면을 막는 것만으로는 부족하다. API 를 직접 호출하면 그대로 뚫린다.
 *
 * <p>판정은 <b>세션 속성</b>으로 한다. 매 요청 DB 를 치면 Argon2 만큼은 아니어도 불필요한 부하다.
 */
@RequiredArgsConstructor
public class PasswordResetGateFilter extends OncePerRequestFilter {

    /** 비밀번호를 바꾸기 위해 반드시 열려 있어야 하는 경로 */
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/v1/auth/password",   // 바꾸러 가는 곳
            "/api/v1/auth/logout",     // 안 바꾸고 나갈 자유
            "/api/v1/auth/me"          // 상태를 확인해야 화면을 그린다
    );

    private final HandlerExceptionResolver resolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isBlocked(request)) {
            // 응답 형식·에러 코드를 GlobalExceptionHandler 한 곳에서만 만들기 위해 위임한다
            resolver.resolveException(request, response, null,
                    new ForbiddenException(AuthErrorCode.AUTH_PASSWORD_RESET_REQUIRED));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlocked(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;   // 미인증 요청은 Security 가 판단한다
        }
        if (!Boolean.TRUE.equals(session.getAttribute(AuthSessionManager.PASSWORD_RESET_REQUIRED))) {
            return false;
        }
        // CORS preflight 는 통과시킨다 — 막으면 브라우저가 본 요청을 보내지도 않는다
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return false;
        }
        return !ALLOWED_PATHS.contains(request.getRequestURI());
    }
}
