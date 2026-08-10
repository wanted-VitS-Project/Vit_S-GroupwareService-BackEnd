package com.group3.vitamins.pagepermission.domain.model;

/**
 * 페이지 접근이 어디서 나왔는지 (`.ai/api/page-permission.md`). 회수 가능 여부·화면 문구의 근거다.
 *
 * <ul>
 *   <li>{@code GRANTED} — ADMIN 이 명시적으로 부여(MEMBER 한정). 회수 가능.</li>
 *   <li>{@code GLOBAL_ROLE} — ADMIN·MASTER 라서 열람됨. 부여 기록 없음. 회수 불가.</li>
 *   <li>{@code ADMIN_ONLY} — ADMIN 전용 페이지(TEMPLATE·ADMIN_CONSOLE). 회수 불가.</li>
 *   <li>{@code DEFAULT} — 기본 노출(HOME·SETTINGS 등, 미부여 상태의 BIDDING·FINANCE 포함). 회수 불가.</li>
 * </ul>
 */
public enum PageAccessSource {
    GRANTED,
    GLOBAL_ROLE,
    ADMIN_ONLY,
    DEFAULT
}
