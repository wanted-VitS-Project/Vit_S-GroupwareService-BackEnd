package com.group3.vitamins.pagepermission.application.result;

/** 페이지 접근 가능자(§3) 항목. revocable=false 는 전역 권한(GLOBAL_ROLE)이라 회수 대상이 아니다. */
public record PageAccessMemberResult(
        String userId,
        String name,
        String departmentPath,
        String jobPositionName,
        String role,
        String permission,
        String source,
        boolean revocable
) {
}
