package com.group3.vitamins.pagepermission.application.result;

import java.time.LocalDate;

/** 페이지 목록(§2) 항목 — 부여 가능한 페이지(BIDDING·FINANCE)와 접근 인원 집계. */
public record PageListItemResult(
        String pageCode,
        String name,
        String description,
        int accessCount,
        int grantedCount,
        int globalRoleCount,
        LocalDate lastModifiedAt
) {
}
