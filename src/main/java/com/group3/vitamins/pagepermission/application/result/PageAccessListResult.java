package com.group3.vitamins.pagepermission.application.result;

import java.util.List;

/** 페이지 접근 가능자 목록(§3) 응답 — 페이지 정보 + 명단 + 집계. */
public record PageAccessListResult(
        String pageCode,
        String name,
        List<PageAccessMemberResult> content,
        int grantedCount,
        int globalRoleCount
) {
}
