package com.group3.vitamins.approval.infrastructure.persistence.row;

import java.time.LocalDateTime;

/**
 * 회차 상세조회(MGT-005)의 결재선 목록 — {@code approval_line}+{@code employee}(+{@code department}
 * +{@code job_position}) 조인 결과. 결재선마다 {@code employeeCatalogPort.findEmployee()}를 따로
 * 호출하던 N+1을 이 조인 한 번으로 대체한다(INV-11 라이브 조회는 유지, 왕복 횟수만 줄인다).
 */
public record ApprovalLineDetailRow(
        Long lineId,
        String approverId,
        String approverName,
        String jobPositionName,
        String departmentName,
        /** 상위 부서명. 최상위 부서 소속이면 {@code null} — {@code auth.UserProfileRow}와 동일 컬럼 구성 */
        String parentDepartmentName,
        int sequenceNo,
        String status,
        String opinion,
        LocalDateTime processedAt,
        boolean approverUnavailable
) {

    /** "기술본부 / 개발팀" 형태. {@code auth.UserProfileRow.departmentPath()}와 동일 로직(SQL이 아닌 코드에서 조립) */
    public String approverDepartment() {
        if (departmentName == null) {
            return null;
        }
        return parentDepartmentName == null ? departmentName : parentDepartmentName + " / " + departmentName;
    }
}
