package com.group3.vitamins.approval.infrastructure.persistence.row;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 결재 도메인이 사원 참여 가능 여부까지 판단하기 위한 employee+account 라이브 조회 행. */
public record ApprovalEmployeeRow(
        String userId,
        String name,
        String role,
        String accountStatus,
        String jobPositionName,
        String departmentName,
        String parentDepartmentName,
        Long companyId,
        LocalDate resignedAt,
        LocalDateTime deletedAt
) {
    public String departmentPath() {
        if (departmentName == null) {
            return null;
        }
        return parentDepartmentName == null ? departmentName : parentDepartmentName + " / " + departmentName;
    }
}
