package com.group3.vitamins.department.application.command;

/**
 * 부서 생성 커맨드 (`.ai/api/department.md` §2). {@code parentId} 가 {@code null} 이면 최상위 부서.
 * {@code role} 은 세션에서 온 전역 권한 — ADMIN 판정에 쓴다 (요청 바디에는 없다).
 */
public record CreateDepartmentCommand(
        String role,
        String name,
        Long parentId
) {
}
