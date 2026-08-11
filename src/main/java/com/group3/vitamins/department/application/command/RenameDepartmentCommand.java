package com.group3.vitamins.department.application.command;

/**
 * 부서명 수정 커맨드 (`.ai/api/department.md` §3). ⛔ 상위 부서는 바꾸지 않는다 — 이름만 바꾼다.
 * {@code role} 은 세션에서 온 전역 권한 — ADMIN 판정에 쓴다.
 */
public record RenameDepartmentCommand(
        String role,
        Long departmentId,
        String name
) {
}
