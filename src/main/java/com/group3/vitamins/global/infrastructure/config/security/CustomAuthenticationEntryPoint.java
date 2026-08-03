package com.group3.vitamins.global.infrastructure.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 인증되지 않은 요청의 401 응답.
 *
 * <p>직접 JSON 을 만들지 않고 {@link HandlerExceptionResolver} 에 넘긴다. 그러면
 * {@code GlobalExceptionHandler} 의 {@code AuthenticationException} 핸들러가 처리하므로
 * <b>응답 포맷과 에러 코드가 한 곳에서만 정의된다.</b> 필터에서 별도로 만들면 두 벌이 되어 어긋난다.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    public CustomAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) {
        resolver.resolveException(request, response, null, authException);
    }
}
