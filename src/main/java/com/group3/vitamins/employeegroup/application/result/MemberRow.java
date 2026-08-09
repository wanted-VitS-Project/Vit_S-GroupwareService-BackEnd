package com.group3.vitamins.employeegroup.application.result;

import java.time.LocalDateTime;

/**
 * 구성원 목록(§5) projection (MyBatis). departmentPath 는 상위부서명+부서명으로 서비스가 조립하므로
 * 여기선 {@code departmentName}·{@code parentDepartmentName} 을 따로 내린다(jobposition 선례). 시스템·퇴사자는 SQL 에서 제외.
 */
public record MemberRow(
        String userId,
        String name,
        String departmentName,
        String parentDepartmentName,
        String jobPositionName,
        LocalDateTime addedAt
) {
}
