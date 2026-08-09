package com.group3.vitamins.pagepermission.domain.model;

/**
 * 페이지 접근 등급 (`.ai/api/page-permission.md`).
 *
 * <p>{@code NONE} 은 <b>my/pages 응답에서만</b> 계산되는 값이다 — "메뉴는 보이되(노출) 접근은 막힘"(부여 전 MEMBER 의 BIDDING·FINANCE).
 * DB {@code page_permission.permission} 은 {@code VIEWER}·{@code EDITOR} 두 값만 저장한다(부여는 접근을 주는 행위라 NONE 을 저장하지 않는다).
 */
public enum PageAccessLevel {
    NONE,
    VIEWER,
    EDITOR;

    /** 부여 요청으로 허용되는 등급인가 (VIEWER·EDITOR). NONE 은 부여 값이 될 수 없다. */
    public static boolean isGrantable(String raw) {
        return "VIEWER".equals(raw) || "EDITOR".equals(raw);
    }
}
