package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.policy.ApprovalListScopePolicy;
import com.group3.vitamins.approval.application.policy.ApprovalRevisionEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalViewPolicy;
import com.group3.vitamins.approval.application.port.ApprovalLineDetailPort;
import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.query.ListApprovalsQuery;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.approval.infrastructure.persistence.mapper.ApprovalListMapper;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 결재관리 목록조회의 <b>회사(테넌트) 범위</b> 검증 (MGT-001~004).
 *
 * <p>게이트(정책)를 통과한 뒤 실제로 반환되는 행을 자르는 유일한 장치다. 특히 {@code scope=all} 은
 * 기안자·결재자 필터가 전부 {@code null} 이라, 회사 조건이 빠지면 <b>전 회사 결재가 그대로 나온다</b>.
 * 인자가 빠져도 컴파일은 통과하므로 여기서 못 박아 둔다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalQueryService — 목록조회 회사 범위")
class ApprovalQueryServiceListScopeTest {

    private static final Long MY_COMPANY = 1L;
    private static final String REQUESTER = "vitas-1234567";

    @Mock
    private ApprovalRevisionEligibilityPolicy revisionEligibilityPolicy;
    @Mock
    private ApprovalViewPolicy viewPolicy;
    @Mock
    private ApprovalListScopePolicy listScopePolicy;
    @Mock
    private EmployeeCatalogPort employeeCatalogPort;
    @Mock
    private FileCatalogPort fileCatalogPort;
    @Mock
    private ApprovalRepository approvalRepository;
    @Mock
    private ApprovalLineDetailPort approvalLineDetailPort;
    @Mock
    private ApprovalListMapper approvalListMapper;
    @Mock
    private BlockCatalogPort blockCatalogPort;
    @Mock
    private CurrentCompanyIdProvider currentCompanyIdProvider;

    @InjectMocks
    private ApprovalQueryService service;

    @Test
    @DisplayName("scope=all 이어도 세션 회사가 매퍼에 전달된다")
    void scopeAllStillPassesCompanyId() {
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(MY_COMPANY);
        when(approvalListMapper.findApprovals(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyInt(), anyInt())).thenReturn(List.of());

        service.listApprovals(query("all"));

        verify(approvalListMapper).countApprovals(eq(MY_COMPANY), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull());
        verify(approvalListMapper).findApprovals(eq(MY_COMPANY), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("scope=drafted 도 본인 필터에 더해 회사가 함께 전달된다")
    void scopeDraftedPassesCompanyIdWithDrafterFilter() {
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(MY_COMPANY);
        when(approvalListMapper.findApprovals(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyInt(), anyInt())).thenReturn(List.of());

        service.listApprovals(query("drafted"));

        verify(approvalListMapper).countApprovals(eq(MY_COMPANY), isNull(), eq(REQUESTER), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("scope=pending 도 결재자 필터에 더해 회사가 함께 전달된다")
    void scopePendingPassesCompanyIdWithActiveApproverFilter() {
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(MY_COMPANY);
        when(approvalListMapper.findApprovals(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyInt(), anyInt())).thenReturn(List.of());

        service.listApprovals(query("pending"));

        verify(approvalListMapper).countApprovals(eq(MY_COMPANY), isNull(), isNull(), isNull(), eq(REQUESTER),
                isNull(), isNull(), isNull(), isNull());
    }

    private ListApprovalsQuery query(String scope) {
        return new ListApprovalsQuery(scope, null, null, null, null, null, null, null, 0, 10, REQUESTER);
    }
}
