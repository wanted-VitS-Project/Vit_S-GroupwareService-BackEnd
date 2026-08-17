package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 결재 조회 3종(MGT-005~007)이 쓰는 조회 권한 검증.
 *
 * <p>2026-08-15 계약 변경 — 열람 범위를 <b>블록이 속한 스텝의 열람 권한자(VIEWER 이상) 전원</b>으로
 * 넓혔다. 블록 카드가 이미 같은 사람들에게 노출되는 마당에 상세만 막는 것은 기밀 보호가 아니라 불편이었고,
 * 자기 차례를 기다리는 결재자가 정작 그 문서를 못 읽는 문제가 있었다.
 *
 * <p>회차 상태는 조회 권한에 영향을 주지 않는다. 블록 목록에서 결재 블록을 볼 수 있는 사람이라면
 * {@code DRAFT}를 포함한 결재 내용도 함께 볼 수 있어야 한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalViewPolicy {

    /**
     * 스텝 참여와 무관하게 회사 안의 결재를 전부 조회할 수 있는 role(MGT-005).
     *
     * <p>⚠️ <b>조회 전용 목록이다.</b> {@code ADMIN} 은 2026-08-17 부터 조회만 열렸고 쓰기는 여전히
     * 막혀 있다 — 쓰기 판정은 {@code ApprovalBlockCatalogAdapter.isStepEditor} 와
     * {@code ApprovalRevisionEligibilityPolicy} 가 별도로 하며 거기서는 {@code MASTER} 만 통과한다.
     * 이 상수를 쓰기 경로에서 재사용하면 인사 role 이 남의 결재를 대행 상신할 수 있게 된다.
     */
    private static final Set<String> FULL_ACCESS_ROLES = Set.of("MASTER", "ADMIN");

    private final EmployeeCatalogPort employeeCatalogPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final BlockCatalogPort blockCatalogPort;

    /** 기안자·대행 기안자·스텝 열람 권한자·결재선 참여자·MASTER·ADMIN만 상태와 무관하게 조회 가능. 아니면 403 */
    public void assertViewable(Approval approval, List<ApprovalLine> lines, String requesterId) {
        assertSameCompany(approval, requesterId);

        EmployeeSummary requester = employeeCatalogPort.findEmployee(requesterId)
                .orElseThrow(() -> new ForbiddenException(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE));
        String role = requester.role();
        // 참여 불가(퇴사·삭제·계정 비활성)는 계속 차단한다. ADMIN 차단은 2026-08-17 에 해제됐다 —
        // 아래 isDrafterSide 의 FULL_ACCESS_ROLES 로 통과한다.
        if (requester.participationUnavailable()) {
            throw new ForbiddenException(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
        }

        if (isDrafterSide(approval, requesterId, role)) {
            return;
        }

        if (blockCatalogPort.canViewBlock(approval.getBlockId(), requesterId, role)) {
            return;
        }

        // 결재선에 이름이 오른 사람은 상태 무관 열람 가능(WAITING 제외 규칙 폐지, 2026-08-15).
        // 프로젝트 멤버십 검사가 면제되는 대표 직책 결재자는 위 스텝 권한으로 안 열려서 이 분기가 필요하다
        // (ApprovalLineEligibilityPolicy 의 MEMBERSHIP_CHECK_EXEMPT_ROLES·대표 직책 참고).
        boolean isParticipant = lines.stream()
                .anyMatch(line -> line.getApproverId().equals(requesterId));
        if (isParticipant) {
            return;
        }

        log.warn("결재 상세조회 - 조회 권한 없음 approvalId={}, requesterId={}", approval.getApprovalId(), requesterId);
        throw new ForbiddenException(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    private boolean isDrafterSide(Approval approval, String requesterId, String role) {
        return approval.getDrafterId().equals(requesterId)
                || requesterId.equals(approval.getActingDrafterId())
                || FULL_ACCESS_ROLES.contains(role);
    }

    /**
     * 회사(테넌트) 경계 검사 — 결재가 연결된 블록의 프로젝트 소속으로 판정한다.
     *
     * <p>role 검사보다 <b>먼저</b> 수행한다. 뒤에 두면 타 회사 {@code MASTER}·{@code ADMIN} 이
     * 이미 통과한 뒤라 의미가 없다.
     *
     * <p>원기안자 라이브 행은 삭제될 수 있으므로 테넌트 기준으로 사용할 수 없다. 블록·스텝·프로젝트
     * 연결은 결재가 살아 있는 동안 유지되므로, 삭제된 기안자의 대행 처리 진입도 회사 격리를 지킨다.
     */
    private void assertSameCompany(Approval approval, String requesterId) {
        if (!blockCatalogPort.isBlockInCompany(
                approval.getBlockId(), currentCompanyIdProvider.currentCompanyId())) {
            log.warn("결재 상세조회 - 회사 불일치 approvalId={}, requesterId={}", approval.getApprovalId(), requesterId);
            throw new ForbiddenException(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
        }
    }
}
