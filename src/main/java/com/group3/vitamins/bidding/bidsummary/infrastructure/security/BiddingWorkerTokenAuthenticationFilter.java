package com.group3.vitamins.bidding.bidsummary.infrastructure.security;

import com.group3.vitamins.auth.domain.exception.AuthErrorCode;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
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

public class BiddingWorkerTokenAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Bidding-Worker-Token";
    private final BiddingWorkerAuthProperties properties;

    public BiddingWorkerTokenAuthenticationFilter(
            BiddingWorkerAuthProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!matches(properties.token(), request.getHeader(HEADER_NAME))) {
            writeUnauthorized(response);
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                "bidding-python-worker",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_BIDDING_WORKER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean matches(String expectedToken, String actualToken) {
        if (!StringUtils.hasText(expectedToken)
                || !StringUtils.hasText(actualToken)) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                actualToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void writeUnauthorized(HttpServletResponse response)
            throws IOException {
        AuthErrorCode errorCode = AuthErrorCode.AUTH_UNAUTHENTICATED;

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"httpStatus\":401,\"message\":\""
                        + errorCode.getMessage()
                        + "\",\"code\":\""
                        + errorCode.getCode()
                        + "\"}"
        );
    }
}