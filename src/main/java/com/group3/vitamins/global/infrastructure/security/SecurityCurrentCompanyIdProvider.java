package com.group3.vitamins.global.infrastructure.security;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import org.springframework.stereotype.Component;

/**
 * {@link CurrentCompanyIdProvider} 의 시큐리티 기반 구현. 세션에 실린 회사 번호를 {@link TenantContext} 로 읽는다.
 * 보안 인프라 의존은 이 어댑터 한 곳에 격리된다 — 애플리케이션 서비스는 포트만 안다.
 */
@Component
public class SecurityCurrentCompanyIdProvider implements CurrentCompanyIdProvider {

    @Override
    public Long currentCompanyId() {
        return TenantContext.currentCompanyId();
    }
}
