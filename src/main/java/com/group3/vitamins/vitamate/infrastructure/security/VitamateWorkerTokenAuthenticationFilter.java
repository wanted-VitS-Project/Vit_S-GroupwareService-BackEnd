package com.group3.vitamins.vitamate.infrastructure.security;

import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

// Python worker가 보낸 내부 토큰을 검증하는 필터
public class VitamateWorkerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Vitamate-Worker-Token";

    private final VitamateWorkerAuthProperties properties;

    public VitamateWorkerTokenAuthenticationFilter(VitamateWorkerAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String expectedToken = properties.token();
        String actualToken = request.getHeader(HEADER_NAME);

        if (!matches(expectedToken, actualToken)) {
            writeUnauthorized(response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "vitamate-python-worker",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_VITAMATE_WORKER"))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // 토큰 비교 시간이 달라지지 않도록 고정 시간 비교를 사용한다.
    private boolean matches(String expectedToken, String actualToken) {
        if (!StringUtils.hasText(expectedToken) || !StringUtils.hasText(actualToken)) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                actualToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    // 내부 인증 실패 응답을 공통 JSON 형태로 내려준다.
    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        VitamateErrorCode errorCode = VitamateErrorCode.VITAMATE_WORKER_UNAUTHORIZED;
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"httpStatus\":401,\"message\":\"" + errorCode.getMessage()
                        + "\",\"code\":\"" + errorCode.getCode() + "\"}"
        );
    }
}
