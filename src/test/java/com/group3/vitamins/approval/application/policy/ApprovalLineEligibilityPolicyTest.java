package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.BlockSummary;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 결재선 등록의 <b>회사(테넌트) 경계</b> 검증 (APR-012).
 *
 * <p>결재 도메인에서 유일하게 <b>쓰기</b>가 회사를 넘을 수 있던 지점이다. 타 회사 사번이 결재선에
 * 들어가면 행이 생기고 결재 요청 알림까지 발송된다.
 *
 * <p>회사 검사는 {@code MASTER}·{@code ADMIN} 의 project member 면제 판정보다 <b>먼저</b> 수행돼야 한다.
 * 순서가 뒤집히면 타 회사 특권 계정이 아무 검사도 안 거치고 통과한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalLineEligibilityPolicy — 회사 경계")
class ApprovalLineEligibilityPolicyTest {

    private static final Long MY_COMPANY = 1L;
    private static final Long OTHER_COMPANY = 2L;

    private static final Long BLOCK_ID = 10L;
    private static final Long PROJECT_ID = 20L;
    private static final String MEMBER = "vitas-1234567";
    private static final String OTHER_COMPANY_MEMBER = "acme-1234567";
    private static final String OTHER_COMPANY_MASTER = "acme-7654321";
    /** 대표는 전역 role 이 아니라 직급(job_position)이다 — 시드 직급명과 같아야 면제가 걸린다 */
    private static final String REPRESENTATIVE = "대표";

    @Mock
    private EmployeeCatalogPort employeeCatalogPort;
    @Mock
    private BlockCatalogPort blockCatalogPort;
    @Mock
    private CurrentCompanyIdProvider currentCompanyIdProvider;

    @InjectMocks
    private ApprovalLineEligibilityPolicy policy;

    @Test
    @DisplayName("타 회사 MASTER 는 거부된다 — 면제 판정보다 회사 검사가 먼저다")
    void otherCompanyMasterIsRejectedBeforeExemption() {
        givenBlock();
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(OTHER_COMPANY_MASTER, "MASTER", OTHER_COMPANY);

        assertThatThrownBy(() -> policy.assertApproversEligible(BLOCK_ID, List.of(OTHER_COMPANY_MASTER)))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);

        // 회사에서 이미 걸렸으므로 project member 조회까지 가지 않는다
        verify(blockCatalogPort, never()).isProjectMember(PROJECT_ID, OTHER_COMPANY_MASTER);
    }

    @Test
    @DisplayName("타 회사 일반 사원도 거부된다")
    void otherCompanyMemberIsRejected() {
        givenBlock();
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(OTHER_COMPANY_MEMBER, "MEMBER", OTHER_COMPANY);

        assertThatThrownBy(() -> policy.assertApproversEligible(BLOCK_ID, List.of(OTHER_COMPANY_MEMBER)))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);
    }

    @Test
    @DisplayName("같은 회사 MASTER 는 project member 가 아니어도 통과한다 (기존 면제 유지)")
    void sameCompanyMasterIsStillExempt() {
        givenBlock();
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(MEMBER, "MASTER", MY_COMPANY);

        List<EmployeeSummary> result = policy.assertApproversEligible(BLOCK_ID, List.of(MEMBER));

        assertThat(result).singleElement()
                .extracting(EmployeeSummary::userId)
                .isEqualTo(MEMBER);
        verify(blockCatalogPort, never()).isProjectMember(PROJECT_ID, MEMBER);
    }

    @Test
    @DisplayName("같은 회사 ADMIN도 결재자로 지정할 수 없다")
    void adminCannotBeApprover() {
        givenBlock();
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(MEMBER, "ADMIN", MY_COMPANY);

        assertThatThrownBy(() -> policy.assertApproversEligible(BLOCK_ID, List.of(MEMBER)))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);
        verify(blockCatalogPort, never()).isProjectMember(PROJECT_ID, MEMBER);
    }

    @Test
    @DisplayName("계정이 비활성인 사원은 프로젝트 참여자여도 결재자로 지정할 수 없다")
    void inactiveEmployeeCannotBeApprover() {
        givenBlock();
        givenCurrentCompany(MY_COMPANY);
        when(employeeCatalogPort.findEmployee(MEMBER)).thenReturn(Optional.of(new EmployeeSummary(
                MEMBER, "홍길동", null, null, "MEMBER", MY_COMPANY, "INACTIVE", null, null)));

        assertThatThrownBy(() -> policy.assertApproversEligible(BLOCK_ID, List.of(MEMBER)))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);
        verify(blockCatalogPort, never()).isProjectMember(PROJECT_ID, MEMBER);
    }

    @Test
    @DisplayName("직급이 대표면 role 이 MEMBER 여도 project member 가 아니어도 통과한다")
    void sameCompanyRepresentativeIsExemptFromMembership() {
        givenBlock();
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(MEMBER, "MEMBER", MY_COMPANY, REPRESENTATIVE);

        List<EmployeeSummary> result = policy.assertApproversEligible(BLOCK_ID, List.of(MEMBER));

        assertThat(result).singleElement()
                .extracting(EmployeeSummary::userId)
                .isEqualTo(MEMBER);
        verify(blockCatalogPort, never()).isProjectMember(PROJECT_ID, MEMBER);
    }

    @Test
    @DisplayName("타 회사 대표는 거부된다 — 소속 면제가 회사 경계까지 열어주지 않는다")
    void otherCompanyRepresentativeIsRejected() {
        givenBlock();
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(OTHER_COMPANY_MEMBER, "MEMBER", OTHER_COMPANY, REPRESENTATIVE);

        assertThatThrownBy(() -> policy.assertApproversEligible(BLOCK_ID, List.of(OTHER_COMPANY_MEMBER)))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);

        verify(blockCatalogPort, never()).isProjectMember(PROJECT_ID, OTHER_COMPANY_MEMBER);
    }

    @Test
    @DisplayName("퇴사한 대표는 거부된다 — 소속 면제는 참여 가능 검사를 면제하지 않는다")
    void resignedRepresentativeIsRejected() {
        givenBlock();
        givenCurrentCompany(MY_COMPANY);
        when(employeeCatalogPort.findEmployee(MEMBER)).thenReturn(Optional.of(new EmployeeSummary(
                MEMBER, "홍길동", REPRESENTATIVE, null, "MEMBER", MY_COMPANY, "ACTIVE",
                LocalDate.of(2026, 8, 12), null)));

        assertThatThrownBy(() -> policy.assertApproversEligible(BLOCK_ID, List.of(MEMBER)))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);

        // 참여 가능 검사에서 걸렸는지 확인 — 이게 없으면 참여 가능 검사를 지워도
        // isProjectMember 기본값(false)이 대신 거부해서 테스트가 통과한다.
        verify(blockCatalogPort, never()).isProjectMember(PROJECT_ID, MEMBER);
    }

    @Test
    @DisplayName("직급명이 '대표'가 아니면 면제되지 않는다 — 소속 검증을 그대로 받는다")
    void nonRepresentativePositionIsNotExempt() {
        givenBlock();
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(MEMBER, "MEMBER", MY_COMPANY, "대표이사");
        when(blockCatalogPort.isProjectMember(PROJECT_ID, MEMBER)).thenReturn(false);

        assertThatThrownBy(() -> policy.assertApproversEligible(BLOCK_ID, List.of(MEMBER)))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);

        // 면제되지 않아 소속 검증까지 내려갔음을 못 박는다 (거부 사유가 소속이라는 뜻)
        verify(blockCatalogPort).isProjectMember(PROJECT_ID, MEMBER);
    }

    @Test
    @DisplayName("같은 회사 일반 사원은 project member 여야 통과한다 (기존 동작 유지)")
    void sameCompanyMemberPassesWhenProjectMember() {
        givenBlock();
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(MEMBER, "MEMBER", MY_COMPANY);
        when(blockCatalogPort.isProjectMember(PROJECT_ID, MEMBER)).thenReturn(true);

        List<EmployeeSummary> result = policy.assertApproversEligible(BLOCK_ID, List.of(MEMBER));

        assertThat(result).singleElement()
                .extracting(EmployeeSummary::companyId)
                .isEqualTo(MY_COMPANY);
    }

    @Test
    @DisplayName("같은 회사여도 project member 가 아니면 거부된다 (기존 동작 유지)")
    void sameCompanyNonMemberIsRejected() {
        givenBlock();
        givenCurrentCompany(MY_COMPANY);
        givenEmployee(MEMBER, "MEMBER", MY_COMPANY);
        when(blockCatalogPort.isProjectMember(PROJECT_ID, MEMBER)).thenReturn(false);

        assertThatThrownBy(() -> policy.assertApproversEligible(BLOCK_ID, List.of(MEMBER)))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);
    }

    @Test
    @DisplayName("존재하지 않는 사번은 타 회사 사번과 같은 코드로 거부된다 — 존재 여부가 드러나지 않는다")
    void unknownApproverIsRejectedWithSameCode() {
        givenBlock();
        givenCurrentCompany(MY_COMPANY);
        when(employeeCatalogPort.findEmployee(OTHER_COMPANY_MEMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policy.assertApproversEligible(BLOCK_ID, List.of(OTHER_COMPANY_MEMBER)))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);
    }

    private void givenBlock() {
        when(blockCatalogPort.findBlock(BLOCK_ID))
                .thenReturn(Optional.of(new BlockSummary(BLOCK_ID, "APPROVAL", 30L, PROJECT_ID, MEMBER)));
    }

    private void givenCurrentCompany(Long companyId) {
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(companyId);
    }

    private void givenEmployee(String userId, String role, Long companyId) {
        givenEmployee(userId, role, companyId, null);
    }

    /** position = 직급명. 대표 판정이 직급명으로 이뤄지므로 테스트에서도 이 인자로 대표를 만든다. */
    private void givenEmployee(String userId, String role, Long companyId, String position) {
        when(employeeCatalogPort.findEmployee(userId))
                .thenReturn(Optional.of(new EmployeeSummary(
                        userId, "홍길동", position, null, role, companyId, "ACTIVE", null, null)));
    }
}
