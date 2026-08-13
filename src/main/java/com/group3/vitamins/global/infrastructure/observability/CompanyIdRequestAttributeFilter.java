package com.group3.vitamins.global.infrastructure.observability;

import com.group3.vitamins.global.infrastructure.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 회사(테넌트) 번호를 {@code SecurityContext}가 아직 살아있는 시점에 요청 속성으로 옮겨 담는다.
 *
 * <p>{@link RequestObservationConvention}이 태그를 계산하는 시점은 요청이 다 끝난 뒤(가장 바깥
 * {@code ServerHttpObservationFilter}의 finally)라, 그때는 안쪽 {@code SecurityContextHolderFilter}가
 * 이미 {@code SecurityContextHolder}를 지운 뒤다. 요청 속성은 {@code SecurityContext}와 달리 응답이 끝날
 * 때까지 그대로 남아있으므로, 이 필터가 지금(인가 통과 시점) 값을 복사해둔다.
 */
public class CompanyIdRequestAttributeFilter extends OncePerRequestFilter {

    public static final String COMPANY_ID_ATTRIBUTE = "companyId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            request.setAttribute(COMPANY_ID_ATTRIBUTE, TenantContext.currentCompanyId());
        } catch (IllegalStateException e) {
            // 비로그인 요청 — 속성을 안 남기고 통과시킨다. RequestObservationConvention 이 "none"으로 처리한다.
        }
        filterChain.doFilter(request, response);
    }
}
