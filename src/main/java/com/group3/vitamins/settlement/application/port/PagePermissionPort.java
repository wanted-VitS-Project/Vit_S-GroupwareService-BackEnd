package com.group3.vitamins.settlement.application.port;

/**
 * 페이지 단위 접근 권한(예: 재무 관리 화면)을 확인하는 아웃바운드 포트.
 * `.ai/api/page-permission.md`(담당 김동현)의 판정 규칙을 따른다 — ADMIN·MASTER는 GLOBAL_ROLE로
 * 항상 접근 가능, MEMBER는 명시적으로 부여된 행이 있어야 한다.
 */
public interface PagePermissionPort {

    boolean hasAccess(String pageCode, String userId, String role);
}
