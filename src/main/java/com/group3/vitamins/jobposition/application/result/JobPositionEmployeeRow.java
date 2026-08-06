package com.group3.vitamins.jobposition.application.result;

/**
 * 직급별 사원 목록의 MyBatis 조회 행 (`.ai/api/job-position.md` §5).
 *
 * <p>{@code departmentPath}("본사 / 개발팀")는 상위 부서명({@code parentDepartmentName})과 부서명을 조립해
 * 만들므로, 여기서는 두 원시 값을 그대로 받고 조립은 서비스가 한다 (사원 목록 {@code EmployeeListRow} 선례).
 */
public record JobPositionEmployeeRow(
        String userId,
        String name,
        String departmentName,
        String parentDepartmentName
) {
}
