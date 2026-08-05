package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
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

    /** MASTER 는 차례와 무관하게 전부 조회 가능(MGT-005). ADMIN 도 서열상 MASTER 를 포함하므로 동일하게 통과 */
    private static final Set<String> FULL_ACCESS_ROLES = Set.of("MASTER", "ADMIN");

    /** "ACTIVE 이상(과거 이력 포함)" — DRAFT(상신 전) · WAITING(차례 안 옴)은 제외 */
    private static final Set<ApprovalLineStatus> VIEWABLE_LINE_STATUSES =
            Set.of(ApprovalLineStatus.ACTIVE, ApprovalLineStatus.APPROVED,
                    ApprovalLineStatus.REJECTED, ApprovalLineStatus.CANCELED);

    private final EmployeeCatalogPort employeeCatalogPort;

    /** 기안자 · ACTIVE 이상 결재자(과거 이력 포함) · MASTER(+ADMIN) 만 조회 가능. 아니면 403 */
    public void assertViewable(Approval approval, List<ApprovalLine> lines, String requesterId) {
        if (approval.getDrafterId().equals(requesterId)) {
            return;
        }

        String role = employeeCatalogPort.findEmployee(requesterId).map(EmployeeSummary::role).orElse(null);
        // Set.of(...).contains(null) 은 NPE를 던진다 — role 이 null(사용자 못 찾음)이면 바로 검사하지 않고 통과
        if (role != null && FULL_ACCESS_ROLES.contains(role)) {
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
}
