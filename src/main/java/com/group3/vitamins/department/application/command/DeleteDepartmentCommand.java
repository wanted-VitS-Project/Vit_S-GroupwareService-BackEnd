package com.group3.vitamins.department.application.command;

/**
 * 부서 삭제 커맨드 (`.ai/api/department.md` §4). {@code role} 은 세션에서 온 전역 권한 — ADMIN 판정에 쓴다.
 */
public record DeleteDepartmentCommand(
        String role,
        Long departmentId
) {
}
