package com.group3.vitamins.employee.application.usecase;

import com.group3.vitamins.employee.application.query.EmployeeListQuery;
import com.group3.vitamins.employee.application.result.EmployeeDetailRow;
import com.group3.vitamins.employee.application.result.EmployeeGroupRow;
import com.group3.vitamins.employee.application.result.EmployeePage;

import java.util.List;

/**
 * 인사관리용 사원 조회 유스케이스 (`employee.md` §1·§2) — 목록·상세. 전부 ADMIN 전용.
 */
public interface EmployeeAdminQueryUseCase {

    /** 사원 목록 조회 (필터·페이징). */
    EmployeePage listEmployees(EmployeeListQuery query);

    /** 사원 상세 조회. 소속 그룹을 함께 담아 반환한다. */
    EmployeeDetail getEmployee(String requesterRole, String userId);

    /** 상세 응답 조립에 필요한 사원 + 그룹 묶음. */
    record EmployeeDetail(EmployeeDetailRow employee, List<EmployeeGroupRow> groups) {
    }
}
