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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** 결재 회차 상세조회(MGT-005)가 쓰는 조회 권한 검증 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalViewPolicy {

    /** MASTER 는 차례와 무관하게 전부 조회 가능(MGT-005). ADMIN은 결재 권한이 없다. */
    private static final Set<String> FULL_ACCESS_ROLES = Set.of("MASTER");

    /** "ACTIVE 이상(과거 이력 포함)" — DRAFT(상신 전) · WAITING(차례 안 옴)은 제외 */
    private static final Set<ApprovalLineStatus> VIEWABLE_LINE_STATUSES =
            Set.of(ApprovalLineStatus.ACTIVE, ApprovalLineStatus.APPROVED,
                    ApprovalLineStatus.REJECTED, ApprovalLineStatus.CANCELED);

    private final EmployeeCatalogPort employeeCatalogPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final BlockCatalogPort blockCatalogPort;

    /** 기안자·대행 기안자·ACTIVE 이상 결재자(과거 이력 포함)·MASTER만 조회 가능. 아니면 403 */
    public void assertViewable(Approval approval, List<ApprovalLine> lines, String requesterId) {
        assertSameCompany(approval, requesterId);

        EmployeeSummary requester = employeeCatalogPort.findEmployee(requesterId).orElse(null);
        String role = requester == null ? null : requester.role();
        if ("ADMIN".equals(role)) {
            throw new ForbiddenException(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
        }

        if (approval.getDrafterId().equals(requesterId)
                || requesterId.equals(approval.getActingDrafterId())) {
            return;
        }

        // Set.of(...).contains(null) 은 NPE를 던진다 — role 이 null(사용자 못 찾음)이면 바로 검사하지 않고 통과
        if (role != null && FULL_ACCESS_ROLES.contains(role)) {
            return;
        }

        // 기안자 또는 대행자가 참여 불가일 때만 스텝 EDITOR에게 상세 열람을 연다.
        // 알림을 클릭한 EDITOR가 대행 선점·결재선 교체 화면에 진입할 수 있어야 한다.
        if (requester != null && !requester.participationUnavailable()
                && requiresDrafterAction(approval)
                && blockCatalogPort.isStepEditor(approval.getBlockId(), requesterId, role)) {
            return;
        }

        boolean isViewableParticipant = lines.stream()
                .anyMatch(line -> line.getApproverId().equals(requesterId)
                        && VIEWABLE_LINE_STATUSES.contains(line.getStatus()));
        if (isViewableParticipant) {
            return;
        }

        log.warn("결재 상세조회 - 조회 권한 없음 approvalId={}, requesterId={}", approval.getApprovalId(), requesterId);
        throw new ForbiddenException(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
    }

    private boolean requiresDrafterAction(Approval approval) {
        if (approval.getStatus() != ApprovalStatus.IN_PROGRESS
                && approval.getStatus() != ApprovalStatus.REJECTED) {
            return false;
        }
        String currentDrafterId = approval.getActingDrafterId() == null
                ? approval.getDrafterId() : approval.getActingDrafterId();
        return employeeCatalogPort.findEmployee(currentDrafterId)
                .map(EmployeeSummary::participationUnavailable)
                .orElse(true);
    }

    /**
     * 회사(테넌트) 경계 검사 — 결재의 회사는 <b>기안자의 소속</b>으로 정한다.
     *
     * <p>role 검사보다 <b>먼저</b> 수행한다. 뒤에 두면 타 회사 {@code MASTER}·{@code ADMIN} 이
     * 이미 통과한 뒤라 의미가 없다.
     *
     * <p>기안자를 못 찾으면(사원 삭제 등) 회사를 확정할 수 없으므로 통과시키지 않는다 —
     * 조회 실패가 열람 허용으로 이어지면 안 된다.
     */
    private void assertSameCompany(Approval approval, String requesterId) {
        Long approvalCompanyId = employeeCatalogPort.findEmployee(approval.getDrafterId())
                .map(EmployeeSummary::companyId)
                .orElse(null);

        if (approvalCompanyId == null || !approvalCompanyId.equals(currentCompanyIdProvider.currentCompanyId())) {
            log.warn("결재 상세조회 - 회사 불일치 approvalId={}, requesterId={}", approval.getApprovalId(), requesterId);
            throw new ForbiddenException(ApprovalErrorCode.APPROVAL_LINE_NOT_VIEWABLE);
        }
    }
}
