package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 결재관리 목록조회 {@code scope} 해석 검증 (MGT-001~004).
 *
 * <p>핵심은 <b>ADMIN 의 기본 scope 승격</b>이다. 403 만 풀고 승격을 빼먹으면 ADMIN 이 빈 목록을 보는데,
 * 예외도 로그도 안 남아 "결재가 없다"와 구분되지 않는다 — 그래서 여기서 못 박아 둔다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalListScopePolicy — scope 해석 · 전체 조회 권한")
class ApprovalListScopePolicyTest {

    private static final Long MY_COMPANY = 1L;
    private static final String REQUESTER = "vitas-1234567";

    @Mock
    private EmployeeCatalogPort employeeCatalogPort;

    @InjectMocks
    private ApprovalListScopePolicy policy;

    @Test
    @DisplayName("ADMIN 의 기본 scope(drafted)는 all 로 해석된다 — 승격 없으면 조용히 0건이다")
    void adminDraftedIsPromotedToAll() {
        givenRole("ADMIN");

        assertThat(policy.resolveScope("drafted", REQUESTER)).isEqualTo("all");
    }

    @Test
    @DisplayName("ADMIN 의 scope=pending 은 승격하지 않는다 — 결재자가 될 수 없어 0건이 정답")
    void adminPendingStaysPending() {
        givenRole("ADMIN");

        assertThat(policy.resolveScope("pending", REQUESTER)).isEqualTo("pending");
    }

    @Test
    @DisplayName("ADMIN 의 scope=all 은 통과한다 — 2026-08-17 조회 허용")
    void adminScopeAllPasses() {
        givenRole("ADMIN");

        assertThat(policy.resolveScope("all", REQUESTER)).isEqualTo("all");
    }

    @Test
    @DisplayName("MASTER 의 scope=all 은 통과한다")
    void masterScopeAllPasses() {
        givenRole("MASTER");

        assertThat(policy.resolveScope("all", REQUESTER)).isEqualTo("all");
    }

    @Test
    @DisplayName("MASTER 의 기본 scope 는 승격되지 않는다 — 본인 기안 건만 본다")
    void masterDraftedStaysDrafted() {
        givenRole("MASTER");

        assertThat(policy.resolveScope("drafted", REQUESTER)).isEqualTo("drafted");
    }

    @Test
    @DisplayName("MEMBER 의 scope=all 은 403")
    void memberScopeAllIsRejected() {
        givenRole("MEMBER");

        assertThatThrownBy(() -> policy.resolveScope("all", REQUESTER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_SCOPE_ALL_FORBIDDEN);
    }

    @Test
    @DisplayName("MEMBER 의 기본 scope 는 그대로 drafted")
    void memberDraftedStaysDrafted() {
        givenRole("MEMBER");

        assertThat(policy.resolveScope("drafted", REQUESTER)).isEqualTo("drafted");
    }

    @Test
    @DisplayName("사원 행을 못 찾으면 scope=all 은 403 — role 이 null 이면 특권 없음으로 본다")
    void unknownRequesterScopeAllIsRejected() {
        when(employeeCatalogPort.findEmployee(REQUESTER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policy.resolveScope("all", REQUESTER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_SCOPE_ALL_FORBIDDEN);
    }

    private void givenRole(String role) {
        when(employeeCatalogPort.findEmployee(REQUESTER))
                .thenReturn(Optional.of(new EmployeeSummary(
                        REQUESTER, "홍길동", null, null, role, MY_COMPANY, "ACTIVE", null, null)));
    }
}
