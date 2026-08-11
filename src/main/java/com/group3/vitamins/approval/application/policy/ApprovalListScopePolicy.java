package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 결재관리 목록조회(MGT-003) — ADMIN 접근 차단과 {@code scope=all} MASTER 권한을 검증한다. */
@Component
@RequiredArgsConstructor
public class ApprovalListScopePolicy {

    private static final Set<String> FULL_ACCESS_ROLES = Set.of("MASTER");

    private final EmployeeCatalogPort employeeCatalogPort;

    public void assertApprovalAccessAllowed(String requesterId) {
        String role = employeeCatalogPort.findEmployee(requesterId).map(EmployeeSummary::role).orElse(null);
        if ("ADMIN".equals(role)) {
            throw new ForbiddenException(ApprovalErrorCode.APPROVAL_SCOPE_ALL_FORBIDDEN);
        }
    }

    /** {@code scope=all} 이면 MASTER만 통과시킨다. 아니면 403(APPROVAL_SCOPE_ALL_FORBIDDEN) */
    public void assertScopeAllAllowed(String requesterId) {
        String role = employeeCatalogPort.findEmployee(requesterId).map(EmployeeSummary::role).orElse(null);
        if (role == null || !FULL_ACCESS_ROLES.contains(role)) {
            throw new ForbiddenException(ApprovalErrorCode.APPROVAL_SCOPE_ALL_FORBIDDEN);
        }
    }
}
