package com.group3.vitamins.global.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 현재 로그인 사용자의 회사(테넌트) 번호를 제공하는 정적 창구.
 *
 * <p>회사 번호는 로그인 시 {@code AuthSessionManager} 가 {@link Authentication#getDetails()} 에
 * {@code Long} 으로 실어둔다. principal 은 사번(String) 그대로라, 기존
 * {@code @AuthenticationPrincipal String userId} 시그니처는 하나도 바뀌지 않는다.
 *
 * <p>RepositoryAdapter 의 조회 필터, Create 커맨드의 company_id 채움 등 회사 격리가 필요한
 * 모든 지점이 여기 한 곳을 통해 회사를 얻는다 (Phase 1).
 */
public final class TenantContext {

    private TenantContext() {
    }

    /**
     * 현재 요청의 회사 번호.
     *
     * @throws IllegalStateException 회사 컨텍스트가 없을 때 — 비로그인 요청이거나,
     *         로그인 세션이 아닌 워커 토큰 경로(예: 비타메이트 워커)에서 호출한 경우.
     *         그런 경로는 회사를 스스로 조회해 처리해야 하며 이 창구를 쓰면 안 된다.
     */
    public static Long currentCompanyId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof Long companyId)) {
            throw new IllegalStateException("회사 컨텍스트가 없습니다 (비로그인 또는 비세션 경로).");
        }
        return companyId;
    }
}
