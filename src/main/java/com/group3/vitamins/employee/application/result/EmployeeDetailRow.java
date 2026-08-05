package com.group3.vitamins.employee.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사원 상세 한 행 (`employee.md` §2). 목록 컬럼 + 상세 전용 컬럼(부서·직급 ID, 연락처, 입사일, 마지막 로그인).
 *
 * <p>{@code isSystem} 은 응답에 나가지 않는다 — 시스템 계정 상세 접근을 403
 * ({@code ACC_SYSTEM_ACCOUNT_NOT_ALLOWED})으로 막기 위한 판정용이다. 소속 그룹은 별도 조회
 * ({@link EmployeeGroupRow})로 붙인다.
 */
public record EmployeeDetailRow(
        String userId,
        String name,
        String email,
        Long departmentId,
        String departmentName,
        String parentDepartmentName,
        Long jobPositionId,
        String jobPositionName,
        String role,
        String accountStatus,
        boolean mustChangePassword,
        String phone,
        LocalDate hiredAt,
        LocalDate resignedAt,
        LocalDateTime lastLoginAt,
        boolean isSystem
) {
}
