package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.BlockSummary;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** 결재선 등록·수정(APR-009~014)이 쓰는 검증 — 존재·개수·순서·결재자 자격 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalLineEligibilityPolicy {

    /** APR-012 — MASTER 는 검증 제외. ADMIN 은 인사 전용 시스템 계정이라 프로젝트 소속이 없어 마찬가지로 제외 */
    private static final Set<String> MEMBERSHIP_CHECK_EXEMPT_ROLES = Set.of("MASTER", "ADMIN");

    private final EmployeeCatalogPort employeeCatalogPort;
    private final BlockCatalogPort blockCatalogPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    /** APR-010 — 결재선은 최소 1명이어야 한다 */
    public void assertNotEmpty(List<?> lines) {
        if (lines.isEmpty()) {
            throw new ValidationException(ApprovalErrorCode.APPROVAL_LINE_EMPTY);
        }
    }

    /** APR-011 — 순서는 1부터 중복·누락 없이 연속돼야 한다 */
    public void assertOrderValid(List<Integer> sequenceNos) {
        List<Integer> sorted = sequenceNos.stream().sorted().toList();
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i) != i + 1) {
                log.warn("결재선 등록 - 순서 무효 sequenceNos={}", sequenceNos);
                throw new ValidationException(ApprovalErrorCode.APPROVAL_LINE_ORDER_INVALID);
            }
        }
    }

    /**
     * APR-012 — 결재자마다 존재 확인 + 회사 일치 확인 + (MASTER·ADMIN 제외) project member 자격을 확인하고,
     * 응답에 필요한 라이브 조회 결과(INV-11)를 입력 순서 그대로 반환한다.
     *
     * <p>회사 검사는 <b>면제 판정보다 먼저</b> 한다. MASTER·ADMIN 면제는 "같은 회사 안에서 소속을 안 따진다"는
     * 뜻이지 회사 경계까지 넘으라는 뜻이 아니다 — 순서를 바꾸면 타 회사 특권 계정이 검사 없이 통과한다.
     */
    public List<EmployeeSummary> assertApproversEligible(Long blockId, List<String> approverIds) {
        Long projectId = blockCatalogPort.findBlock(blockId).map(BlockSummary::projectId).orElse(null);
        Long companyId = currentCompanyIdProvider.currentCompanyId();

        return approverIds.stream()
                .map(approverId -> {
                    EmployeeSummary employee = employeeCatalogPort.findEmployee(approverId)
                            .orElseThrow(() -> {
                                log.warn("결재선 등록 - 존재하지 않는 사번 approverId={}", approverId);
                                return new ValidationException(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);
                            });

                    if (!companyId.equals(employee.companyId())) {
                        // 사번 존재 여부가 드러나지 않도록 "존재하지 않는 사번"과 같은 코드로 응답한다
                        log.warn("결재선 등록 - 타 회사 사원 approverId={}", approverId);
                        throw new ValidationException(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);
                    }

                    boolean exempt = MEMBERSHIP_CHECK_EXEMPT_ROLES.contains(employee.role());
                    if (!exempt && !blockCatalogPort.isProjectMember(projectId, approverId)) {
                        log.warn("결재선 등록 - project member 아님 approverId={}", approverId);
                        throw new ValidationException(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);
                    }
                    return employee;
                })
                .toList();
    }
}
