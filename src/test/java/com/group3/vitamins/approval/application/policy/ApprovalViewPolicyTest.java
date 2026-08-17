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
 * 결재 조회 권한 검증 — <b>회사(테넌트) 경계</b>와 <b>스텝 열람 권한 기준</b>(2026-08-15 계약 변경).
 *
 * <p>결재의 회사는 원기안자 라이브 행이 아니라 연결된 블록의 프로젝트로 정한다. 핵심은 회사 검사가
 * role 검사보다 <b>앞</b>이라는 것 — 순서가 뒤집히면 타 회사 {@code MASTER}·{@code ADMIN} 이 통과한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalViewPolicy — 회사 경계 · 스텝 열람 권한")
class ApprovalViewPolicyTest {

    private static final Long MY_COMPANY = 1L;
    private static final Long APPROVAL_ID = 100L;
    private static final Long BLOCK_ID = 10L;
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
        when(blockCatalogPort.isBlockInCompany(BLOCK_ID, MY_COMPANY)).thenReturn(false);

        assertThatThrownBy(() -> policy.assertViewable(approval(), List.of(), OTHER_COMPANY_MASTER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    @Test
    @DisplayName("같은 회사 MASTER 는 스텝 참여와 무관하게 통과한다")
    void sameCompanyMasterPasses() {
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(APPROVER, "MASTER", MY_COMPANY);

        assertThatCode(() -> policy.assertViewable(approval(), List.of(), APPROVER))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("같은 회사 ADMIN도 결재 상세를 조회할 수 없다")
    void adminIsRejected() {
        givenCurrentCompany(MY_COMPANY);
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
    @DisplayName("결재선에 없어도 스텝 열람 권한(VIEWER 이상)이 있으면 통과한다 — 2026-08-15 확대")
    void stepViewerPasses() {
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(APPROVER, "MEMBER", MY_COMPANY);
        when(blockCatalogPort.canViewBlock(BLOCK_ID, APPROVER, "MEMBER")).thenReturn(true);

        assertThatCode(() -> policy.assertViewable(approval(), List.of(), APPROVER))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("WAITING 결재자도 통과한다 — WAITING 403 규칙 폐기")
    void waitingApproverPasses() {
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(APPROVER, "MEMBER", MY_COMPANY);
        when(blockCatalogPort.canViewBlock(BLOCK_ID, APPROVER, "MEMBER")).thenReturn(true);

        assertThatCode(() -> policy.assertViewable(
                approval(), List.of(line(APPROVER, ApprovalLineStatus.WAITING)), APPROVER))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("스텝 권한이 없어도 결재선에 이름이 있으면 통과한다 — 멤버십 면제 대표 직책 결재자용")
    void nonMemberApproverOnLinePasses() {
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(APPROVER, "MEMBER", MY_COMPANY);
        when(blockCatalogPort.canViewBlock(BLOCK_ID, APPROVER, "MEMBER")).thenReturn(false);

        assertThatCode(() -> policy.assertViewable(
                approval(), List.of(line(APPROVER, ApprovalLineStatus.WAITING)), APPROVER))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("스텝 권한도 없고 결재선에도 없으면 403 — 스텝 오버라이드 NONE 포함")
    void outsiderIsRejected() {
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(APPROVER, "MEMBER", MY_COMPANY);
        when(blockCatalogPort.canViewBlock(BLOCK_ID, APPROVER, "MEMBER")).thenReturn(false);

        assertThatThrownBy(() -> policy.assertViewable(approval(), List.of(), APPROVER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    @Test
    @DisplayName("요청자를 찾을 수 없으면 같은 회사 결재여도 403")
    void unknownRequesterIsRejected() {
        givenCurrentCompany(MY_COMPANY);
        when(employeeCatalogPort.findEmployee(DRAFTER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policy.assertViewable(approval(), List.of(), DRAFTER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    @Test
    @DisplayName("결재 블록이 타 회사 프로젝트 소속이면 요청자가 누구든 403")
    void approvalOfOtherCompanyIsRejected() {
        givenCurrentCompany(MY_COMPANY);
        when(blockCatalogPort.isBlockInCompany(BLOCK_ID, MY_COMPANY)).thenReturn(false);

        assertThatThrownBy(() -> policy.assertViewable(approval(), List.of(), DRAFTER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    @Test
    @DisplayName("참여 불가(퇴사·비활성) 사원은 스텝 권한이 있어도 403")
    void unavailableEmployeeIsRejected() {
        givenCurrentCompany(MY_COMPANY);
        when(employeeCatalogPort.findEmployee(APPROVER))
                .thenReturn(Optional.of(new EmployeeSummary(
                        APPROVER, "퇴사자", null, null, "MEMBER", MY_COMPANY,
                        "INACTIVE", LocalDate.of(2026, 8, 11), null)));

        assertThatThrownBy(() -> policy.assertViewable(approval(), List.of(), APPROVER))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    private void givenCurrentCompany(Long companyId) {
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(companyId);
        when(blockCatalogPort.isBlockInCompany(BLOCK_ID, companyId)).thenReturn(true);
    }

    private void givenEmployee(String userId, String role, Long companyId) {
        when(employeeCatalogPort.findEmployee(userId))
                .thenReturn(Optional.of(new EmployeeSummary(
                        userId, "홍길동", null, null, role, companyId, "ACTIVE", null, null)));
    }

    private Approval approval() {
        return Approval.reconstruct(APPROVAL_ID, BLOCK_ID, DRAFTER, null, ApprovalStatus.IN_PROGRESS,
                1, null, null, null, null);
    }

    private ApprovalLine line(String approverId, ApprovalLineStatus status) {
        return ApprovalLine.reconstruct(200L, 300L, approverId, 1, status, null, null, null, null);
    }
}
