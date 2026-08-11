package com.group3.vitamins.finance.infrastructure.adapter;

import com.group3.vitamins.finance.application.port.PagePermissionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ⚠️ 임시 어댑터 — PagePermission 도메인(담당 김동현)에 정식 서비스가 아직 없어서,
 * finance 도메인이 소비자로서 {@code page_permission} 테이블을 마이바티스로 직접 조회한다
 * (settlement 도메인의 동일 임시 구현 선례를 그대로 따름). 정식 PagePermission 유스케이스가 생기면
 * 이 클래스 내부 구현만 그 포트 호출로 교체하면 된다.
 *
 * <p>클래스명에 finance 접두어를 붙인 이유는 settlement의 동명 어댑터(PagePermissionMapperAdapter)와
 * 컴포넌트 스캔 시 빈 이름이 겹쳐 기동이 실패했기 때문이다(2026-08-10 확인).
 *
 * <p>판정 규칙은 {@code .ai/api/page-permission.md} 를 그대로 따른다: `ADMIN`·`MASTER`는 GLOBAL_ROLE로
 * 항상 접근 가능(부여 기록 불필요), `MEMBER`는 {@code page_permission}에 그 pageCode로 부여된 행이 있어야 한다.
 */
@Component
@RequiredArgsConstructor
public class FinancePagePermissionMapperAdapter implements PagePermissionPort {

    private static final String GLOBAL_ROLE_ADMIN = "ADMIN";
    private static final String GLOBAL_ROLE_MASTER = "MASTER";

    private final FinancePagePermissionMapper financePagePermissionMapper;

    @Override
    public boolean hasAccess(String pageCode, String userId, String role) {
        if (GLOBAL_ROLE_ADMIN.equals(role) || GLOBAL_ROLE_MASTER.equals(role)) {
            return true;
        }
        return financePagePermissionMapper.existsGrant(pageCode, userId);
    }

    @Override
    public boolean hasEditAccess(String pageCode, String userId, String role) {
        if (GLOBAL_ROLE_ADMIN.equals(role) || GLOBAL_ROLE_MASTER.equals(role)) {
            return true;
        }
        return financePagePermissionMapper.existsEditGrant(pageCode, userId);
    }
}
