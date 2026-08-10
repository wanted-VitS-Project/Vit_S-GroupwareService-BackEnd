package com.group3.vitamins.pagepermission.application.result;

/** 내 페이지 목록(§1) 항목 — 노출되는 페이지 하나. permission=NONE 이면 노출되나 접근 불가. */
public record MyPageResult(
        String pageCode,
        String name,
        String permission,
        String source
) {
}
