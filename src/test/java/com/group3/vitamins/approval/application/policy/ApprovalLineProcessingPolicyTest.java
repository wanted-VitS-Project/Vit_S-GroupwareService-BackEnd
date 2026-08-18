package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalLineProcessingPolicyTest {

    private static final String REQUESTER = "EMP001";

    @Mock
    private ApprovalRepository approvalRepository;
    @Mock
    private EmployeeCatalogPort employeeCatalogPort;

    @InjectMocks
    private ApprovalLineProcessingPolicy policy;

    @Test
    void missingEmployeeCannotProcessApprovalLine() {
        when(employeeCatalogPort.findEmployee(REQUESTER)).thenReturn(Optional.empty());

        assertForbidden();
    }

    @Test
    void participationUnavailableEmployeeCannotProcessApprovalLine() {
        when(employeeCatalogPort.findEmployee(REQUESTER)).thenReturn(Optional.of(
                new EmployeeSummary(REQUESTER, "퇴사자", null, null, "MEMBER", 1L,
                        "INACTIVE", null, null)));

        assertForbidden();
    }

    /**
     * 2026-08-18 — ADMIN 도 결재선에 지정될 수 있게 되면서 승인·반려 차단을 풀었다.
     * 여기를 막으면 지정된 ADMIN 이 처리를 못 해 결재가 영구 정지한다.
     */
    @Test
    void adminCanProcessOwnActiveApprovalLine() {
        when(employeeCatalogPort.findEmployee(REQUESTER)).thenReturn(Optional.of(
                new EmployeeSummary(REQUESTER, "관리자", null, null, "ADMIN", 1L,
                        "ACTIVE", null, null)));
        ApprovalLine line = ApprovalLine.reconstruct(1L, 2L, REQUESTER, 1,
                ApprovalLineStatus.ACTIVE, null, null, null, null);
        when(approvalRepository.findLineByIdForUpdate(1L)).thenReturn(Optional.of(line));

        assertThat(policy.getActiveOwnedLineOrThrow(1L, REQUESTER)).isSameAs(line);
    }

    @Test
    void participationUnavailableAdminCannotProcessApprovalLine() {
        when(employeeCatalogPort.findEmployee(REQUESTER)).thenReturn(Optional.of(
                new EmployeeSummary(REQUESTER, "관리자", null, null, "ADMIN", 1L,
                        "INACTIVE", null, null)));

        assertForbidden();
    }

    private void assertForbidden() {
        assertThatThrownBy(() -> policy.getActiveOwnedLineOrThrow(1L, REQUESTER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_FORBIDDEN);
        verify(approvalRepository, never()).findLineByIdForUpdate(1L);
    }
}
