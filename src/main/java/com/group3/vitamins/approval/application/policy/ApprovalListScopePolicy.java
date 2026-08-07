package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 결재관리 목록조회(MGT-003) — {@code scope=all} 권한 검증. {@code ApprovalViewPolicy}와 동일 기준(MASTER·ADMIN)을 쓴다. */
@Component
@RequiredArgsConstructor
public class ApprovalListScopePolicy {

    private static final Set<String> FULL_ACCESS_ROLES = Set.of("MASTER", "ADMIN");

    private final EmployeeCatalogPort employeeCatalogPort;

    /** {@code scope=all} 이면 MASTER·ADMIN 만 통과시킨다. 아니면 403(APPROVAL_SCOPE_ALL_FORBIDDEN) */
    public void assertScopeAllAllowed(String requesterId) {
        String role = employeeCatalogPort.findEmployee(requesterId).map(EmployeeSummary::role).orElse(null);
        if (role == null || !FULL_ACCESS_ROLES.contains(role)) {
            throw new ForbiddenException(ApprovalErrorCode.APPROVAL_SCOPE_ALL_FORBIDDEN);
        }
    }
}
