package com.group3.vitamins.employee.application.result;

/**
 * 사원이 속한 그룹 한 행 (`employee.md` §2 {@code data.groups[]}).
 *
 * @param groupId 그룹 번호
 * @param name    그룹명
 */
public record EmployeeGroupRow(
        Long groupId,
        String name
) {
}
