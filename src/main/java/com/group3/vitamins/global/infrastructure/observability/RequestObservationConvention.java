package com.group3.vitamins.global.infrastructure.observability;

import io.micrometer.common.KeyValues;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/**
 * {@code http.server.requests} 지표에 {@code company_id}·{@code domain} 태그를 붙인다.
 *
 * <p>{@code company_id} — 그라파나 Phase5(운영: 회사별 트래픽·CPU 추정)와 Phase4 부하테스트(회사 몇 개까지
 * 버티는지)에서 공통으로 쓴다. ⚠️ 여기서 {@code TenantContext.currentCompanyId()}(SecurityContextHolder)를
 * 직접 읽으면 안 된다 — 이 메서드는 {@code ServerHttpObservationFilter}(order {@code MIN_VALUE+1}, 전체를
 * 감싸는 가장 바깥 필터)가 요청을 끝맺을 때 호출되는데, 그 시점엔 안쪽의 {@code SecurityContextHolderFilter}가
 * 자기 {@code finally}에서 이미 {@code SecurityContextHolder.clearContext()}를 호출한 뒤라 항상 비로그인으로
 * 보인다. 그래서 {@link CompanyIdRequestAttributeFilter}가 컨텍스트가 살아있는 시점에 요청 속성으로 옮겨둔 값을
 * 읽는다.
 *
 * <p>{@code domain} — Phase4 부하테스트 중 "어떤 도메인이 무거운지"를 담당자 배정 단위로 보기 위함.
 * {@code uri}만으로는 엔드포인트가 40여 개로 흩어져 있어 도메인 단위 롤업이 안 된다. 컨트롤러가 속한
 * 패키지의 첫 세그먼트(예: {@code issue}, {@code project})를 그대로 쓴다 — {@code project.stage}처럼
 * 애그리게이트가 서브패키지로 나뉜 경우도 전부 {@code project} 하나로 묶인다(.ai/ARCHITECTURE.md §2-1).
 * 이건 {@code HandlerMapping}이 세팅하는 요청 속성이라 SecurityContext와 달리 끝까지 살아있다 — 위 함정이 없다.
 *
 * <p>이 빈이 컨텍스트에 있으면 {@code WebMvcObservationAutoConfiguration}이
 * {@code ObjectProvider<ServerRequestObservationConvention>}로 기본 convention 대신 이걸 주입한다.
 */
@Component
public class RequestObservationConvention extends DefaultServerRequestObservationConvention {

    private static final String BASE_PACKAGE = "com.group3.vitamins.";
    private static final String UNKNOWN = "unknown";

    @Override
    public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        return super.getLowCardinalityKeyValues(context)
                .and("company_id", resolveCompanyId(context))
                .and("domain", resolveDomain(context));
    }

    private String resolveCompanyId(ServerRequestObservationContext context) {
        HttpServletRequest request = context.getCarrier();
        if (request == null) {
            return "none";
        }
        Object companyId = request.getAttribute(CompanyIdRequestAttributeFilter.COMPANY_ID_ATTRIBUTE);
        return companyId == null ? "none" : String.valueOf(companyId);
    }

    private String resolveDomain(ServerRequestObservationContext context) {
        HttpServletRequest request = context.getCarrier();
        if (request == null) {
            return UNKNOWN;
        }

        // 핸들러 매핑 이후에만 설정된다 — 정적 리소스·404·actuator 는 HandlerMethod 가 아니다.
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return UNKNOWN;
        }

        String packageName = handlerMethod.getBeanType().getPackageName();
        if (!packageName.startsWith(BASE_PACKAGE)) {
            return UNKNOWN;
        }

        String rest = packageName.substring(BASE_PACKAGE.length());
        int dot = rest.indexOf('.');
        return dot > 0 ? rest.substring(0, dot) : rest;
    }
}
