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

    /**
     * APR-012 — 프로젝트 소속 검증을 면제받는 role. {@code ADMIN} 은 2026-08-18 에 추가됐다.
     *
     * <p>⚠️ <b>차단 해제와 이 면제는 한 쌍이다.</b> {@code ADMIN} 은 {@code project_member} 에 등록되지
     * 않으므로(`PERMISSION.md` §2-4) 아래 role 차단만 풀고 여기에 안 넣으면 소속 검증에서 같은
     * 에러코드로 다시 걸린다 — 증상이 똑같아 "안 고쳐졌다"로 보인다.
     */
    private static final Set<String> MEMBERSHIP_CHECK_EXEMPT_ROLES = Set.of("MASTER", "ADMIN");

    /**
     * 대표도 프로젝트 소속 검증에서 제외한다. 대표는 전역 role 이 {@code MEMBER} 라 role 로 구분되지 않아
     * <b>직급명</b>({@code job_position.name})으로 판정한다 — 회사가 직급을 직접 만들기 때문에
     * 이름을 {@code '대표이사'} 등으로 지은 회사에서는 이 면제가 걸리지 않는다.
     */
    private static final String REPRESENTATIVE_JOB_POSITION = "대표";

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
     * APR-012 — 결재자마다 존재·참여 가능·회사 일치 확인 + (MASTER·ADMIN·직급 대표 제외) project member 자격을
     * 확인하고, 응답에 필요한 라이브 조회 결과(INV-11)를 입력 순서 그대로 반환한다.
     *
     * <p>회사 검사는 <b>면제 판정보다 먼저</b> 한다. 면제는 "같은 회사 안에서 소속을 안 따진다"는
     * 뜻이지 회사 경계까지 넘으라는 뜻이 아니다 — 순서를 바꾸면 타 회사 특권 계정이 검사 없이 통과한다.
     * 참여 가능(퇴사·삭제·비활성) 검사도 면제 대상이 아니다 — 퇴사한 대표는 여전히 지정할 수 없다.
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

                    if (employee.participationUnavailable()) {
                        log.warn("결재선 등록 - 참여 불가 사원 approverId={}", approverId);
                        throw new ValidationException(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);
                    }

                    boolean exempt = MEMBERSHIP_CHECK_EXEMPT_ROLES.contains(employee.role())
                            || REPRESENTATIVE_JOB_POSITION.equals(employee.position());
                    if (!exempt && !blockCatalogPort.isProjectMember(projectId, approverId)) {
                        log.warn("결재선 등록 - project member 아님 approverId={}", approverId);
                        throw new ValidationException(ApprovalErrorCode.APPROVAL_LINE_APPROVER_NOT_MEMBER);
                    }
                    return employee;
                })
                .toList();
    }

    public boolean isParticipationUnavailable(String userId) {
        return employeeCatalogPort.findEmployee(userId)
                .map(EmployeeSummary::participationUnavailable)
                .orElse(true);
    }
}
