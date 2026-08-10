package com.group3.vitamins.global.application.tenant;

/**
 * 현재 요청의 회사(테넌트) 번호를 제공하는 <b>애플리케이션 아웃바운드 포트</b>.
 *
 * <p>애플리케이션 서비스가 보안 인프라({@code SecurityContext}/{@code TenantContext})에 직접 의존하지 않도록,
 * "현재 회사 ID" 획득을 이 포트로 추상화한다. 구현체는 인프라 계층의
 * {@code SecurityCurrentCompanyIdProvider} 가 담당한다 (아키텍처 §2 — 외부 구현체 격리).
 *
 * <p>덕분에 서비스 단위테스트는 SecurityContext 를 세팅할 필요 없이 이 포트를 목으로 대체하면 된다.
 */
public interface CurrentCompanyIdProvider {

    /**
     * 현재 로그인 사용자의 회사 번호.
     *
     * @throws IllegalStateException 회사 컨텍스트가 없을 때(비로그인·비세션 경로) — 구현체가 던진다.
     */
    Long currentCompanyId();
}
