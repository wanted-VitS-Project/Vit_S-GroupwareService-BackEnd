package com.group3.vitamins.pagepermission.application.result;

/** §4 부여 검증·§5 회수 후 판정용 — 요청 사번의 role·시스템계정 여부(회사 범위). 없는/타사 사번은 결과에 빠진다. */
public record EmployeeRoleRow(
        String userId,
        String role,
        boolean isSystem
) {
}
