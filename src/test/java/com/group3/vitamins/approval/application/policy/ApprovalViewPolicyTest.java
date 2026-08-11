package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 결재 조회 권한의 <b>회사(테넌트) 경계</b> 검증.
 *
 * <p>결재의 회사는 기안자 소속으로 정한다. 핵심은 회사 검사가 role 검사보다 <b>앞</b>이라는 것 —
 * 순서가 뒤집히면 타 회사 {@code MASTER}·{@code ADMIN} 이 그대로 통과한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalViewPolicy — 회사 경계")
class ApprovalViewPolicyTest {

    private static final Long MY_COMPANY = 1L;
    private static final Long OTHER_COMPANY = 2L;

    private static final Long APPROVAL_ID = 100L;
    private static final String DRAFTER = "vitas-1234567";
    private static final String APPROVER = "vitas-7654321";
    private static final String OTHER_COMPANY_MASTER = "acme-1234567";

    @Mock
    private EmployeeCatalogPort employeeCatalogPort;
    @Mock
    private CurrentCompanyIdProvider currentCompanyIdProvider;
    @Mock
    private BlockCatalogPort blockCatalogPort;

    @InjectMocks
    private ApprovalViewPolicy policy;

    @Test
    @DisplayName("타 회사 MASTER 는 approvalId 를 알아도 403 — 회사 검사가 role 검사보다 먼저다")
    void otherCompanyMasterIsRejected() {
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(DRAFTER, "MEMBER", MY_COMPANY);

        assertThatThrownBy(() -> policy.assertViewable(approval(), List.of(), OTHER_COMPANY_MASTER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    @Test
    @DisplayName("같은 회사 MASTER 는 차례와 무관하게 통과한다 (기존 동작 유지)")
    void sameCompanyMasterPasses() {
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(DRAFTER, "MEMBER", MY_COMPANY);
        givenEmployee(APPROVER, "MASTER", MY_COMPANY);

        assertThatCode(() -> policy.assertViewable(approval(), List.of(), APPROVER))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("같은 회사 ADMIN도 결재 상세를 조회할 수 없다")
    void adminIsRejected() {
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(DRAFTER, "MEMBER", MY_COMPANY);
        givenEmployee(APPROVER, "ADMIN", MY_COMPANY);

        assertThatThrownBy(() -> policy.assertViewable(approval(), List.of(), APPROVER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    @Test
    @DisplayName("기안자 본인은 통과한다")
    void drafterPasses() {
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(DRAFTER, "MEMBER", MY_COMPANY);

        assertThatCode(() -> policy.assertViewable(approval(), List.of(), DRAFTER))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("같은 회사의 ACTIVE 결재자는 통과한다 (기존 동작 유지)")
    void sameCompanyActiveApproverPasses() {
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(DRAFTER, "MEMBER", MY_COMPANY);
        givenEmployee(APPROVER, "MEMBER", MY_COMPANY);

        assertThatCode(() -> policy.assertViewable(
                approval(), List.of(line(APPROVER, ApprovalLineStatus.ACTIVE)), APPROVER))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("같은 회사여도 WAITING 결재자는 403 (기존 동작 유지)")
    void sameCompanyWaitingApproverIsRejected() {
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(DRAFTER, "MEMBER", MY_COMPANY);
        givenEmployee(APPROVER, "MEMBER", MY_COMPANY);

        assertThatThrownBy(() -> policy.assertViewable(
                approval(), List.of(line(APPROVER, ApprovalLineStatus.WAITING)), APPROVER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    @Test
    @DisplayName("기안자를 못 찾으면 회사를 확정할 수 없으므로 403 (fail-closed)")
    void unknownDrafterIsRejected() {
        // 세션 회사를 스텁하지 않는다 — 기안자를 못 찾은 시점에 단락되어 조회조차 하지 않는 게 정상이다
        when(employeeCatalogPort.findEmployee(DRAFTER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policy.assertViewable(approval(), List.of(), DRAFTER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    @Test
    @DisplayName("기안자가 타 회사면 요청자가 누구든 403 — 회사가 다른 결재는 존재하지 않는 것과 같다")
    void approvalOfOtherCompanyIsRejected() {
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(DRAFTER, "MEMBER", OTHER_COMPANY);

        assertThatThrownBy(() -> policy.assertViewable(approval(), List.of(), DRAFTER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    @Test
    @DisplayName("기안자 참여 불가 시 같은 스텝 EDITOR는 알림 대상 결재를 조회할 수 있다")
    void stepEditorPassesWhenDrafterUnavailable() {
        givenCurrentCompany(MY_COMPANY);
        when(employeeCatalogPort.findEmployee(DRAFTER))
                .thenReturn(Optional.of(new EmployeeSummary(
                        DRAFTER, "퇴사자", null, null, "MEMBER", MY_COMPANY,
                        "INACTIVE", LocalDate.of(2026, 8, 11), null)));
        givenEmployee(APPROVER, "MEMBER", MY_COMPANY);
        when(blockCatalogPort.isStepEditor(10L, APPROVER, "MEMBER")).thenReturn(true);

        assertThatCode(() -> policy.assertViewable(approval(), List.of(), APPROVER))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("기안자가 유효하면 스텝 EDITOR여도 기존 조회 범위를 넓히지 않는다")
    void stepEditorRejectedWhenDrafterAvailable() {
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(DRAFTER, "MEMBER", MY_COMPANY);
        givenEmployee(APPROVER, "MEMBER", MY_COMPANY);

        assertThatThrownBy(() -> policy.assertViewable(approval(), List.of(), APPROVER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    @Test
    @DisplayName("기안자가 이탈해도 완료 결재는 스텝 EDITOR에게 새로 공개하지 않는다")
    void completedApprovalRemainsHiddenFromStepEditor() {
        givenCurrentCompany(MY_COMPANY);
        when(employeeCatalogPort.findEmployee(DRAFTER))
                .thenReturn(Optional.of(new EmployeeSummary(
                        DRAFTER, "퇴사자", null, null, "MEMBER", MY_COMPANY,
                        "INACTIVE", LocalDate.of(2026, 8, 11), null)));
        givenEmployee(APPROVER, "MEMBER", MY_COMPANY);
        Approval completed = Approval.reconstruct(
                APPROVAL_ID, 10L, DRAFTER, null, ApprovalStatus.COMPLETED,
                1, null, null, null, null);

        assertThatThrownBy(() -> policy.assertViewable(completed, List.of(), APPROVER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    @Test
    @DisplayName("기안자와 EDITOR가 모두 참여 불가면 EDITOR 예외 조회를 허용하지 않는다")
    void unavailableStepEditorIsRejected() {
        givenCurrentCompany(MY_COMPANY);
        when(employeeCatalogPort.findEmployee(DRAFTER))
                .thenReturn(Optional.of(new EmployeeSummary(
                        DRAFTER, "퇴사 기안자", null, null, "MEMBER", MY_COMPANY,
                        "INACTIVE", LocalDate.of(2026, 8, 11), null)));
        when(employeeCatalogPort.findEmployee(APPROVER))
                .thenReturn(Optional.of(new EmployeeSummary(
                        APPROVER, "퇴사 편집자", null, null, "MEMBER", MY_COMPANY,
                        "INACTIVE", LocalDate.of(2026, 8, 11), null)));

        assertThatThrownBy(() -> policy.assertViewable(approval(), List.of(), APPROVER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    private void givenCurrentCompany(Long companyId) {
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(companyId);
    }

    private void givenEmployee(String userId, String role, Long companyId) {
        when(employeeCatalogPort.findEmployee(userId))
                .thenReturn(Optional.of(new EmployeeSummary(
                        userId, "홍길동", null, null, role, companyId, "ACTIVE", null, null)));
    }

    private Approval approval() {
        return Approval.reconstruct(APPROVAL_ID, 10L, DRAFTER, null, ApprovalStatus.IN_PROGRESS,
                1, null, null, null, null);
    }

    private ApprovalLine line(String approverId, ApprovalLineStatus status) {
        return ApprovalLine.reconstruct(200L, 300L, approverId, 1, status, null, null, null, null);
    }
}
