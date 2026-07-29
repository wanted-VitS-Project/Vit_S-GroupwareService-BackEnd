package com.group3.vitamins.global.infrastructure.logging;

import com.group3.vitamins.global.infrastructure.web.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";

    private final ClientIpResolver clientIpResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String traceId = UUID.randomUUID().toString();

        MDC.put(TRACE_ID, traceId);

        log.info(
                "event=request_started method={} uri={}",
                request.getMethod(),
                request.getRequestURI()
        );

        try {
            filterChain.doFilter(request, response);
            logRequestCompleted(request, response, traceId, startNanos);
        } catch (IOException | ServletException | RuntimeException e) {
            logRequestFailed(request, response, traceId, startNanos, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    private void logRequestCompleted(
            HttpServletRequest request,
            HttpServletResponse response,
            String traceId,
            long startNanos
    ) {
        log.info(
                "event=request_completed traceId={} method={} uri={} status={} durationMs={} userId={} role={} clientIp={}",
                traceId,
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                elapsedMillis(startNanos),
                getUserId(),
                getRole(),
                clientIpResolver.resolve(request)
        );
    }

    private void logRequestFailed(
            HttpServletRequest request,
            HttpServletResponse response,
            String traceId,
            long startNanos,
            Exception exception
    ) {
        log.warn(
                "event=request_failed traceId={} method={} uri={} status={} durationMs={} userId={} role={} clientIp={} exception={} message={}",
                traceId,
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                elapsedMillis(startNanos),
                getUserId(),
                getRole(),
                clientIpResolver.resolve(request),
                exception.getClass().getSimpleName(),
                exception.getMessage()
        );
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }

        if ("anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            return "anonymous";
        }

        return authentication.getName();
    }

    private String getRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }

        return authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> removeRolePrefix(authority.getAuthority()))
                .orElse("unknown");
    }

    private String removeRolePrefix(String authority) {
        if (authority == null) {
            return "unknown";
        }

        if (authority.startsWith("ROLE_")) {
            return authority.substring("ROLE_".length());
        }

        return authority;
    }
}
