package com.group3.vitamins.finance.application.port;

/**
 * 페이지 단위 접근 권한(재무 관리 화면)을 확인하는 아웃바운드 포트.
 * `.ai/api/page-permission.md`(담당 김동현)의 판정 규칙을 따른다 — ADMIN·MASTER는 GLOBAL_ROLE로
 * 항상 접근 가능, MEMBER는 명시적으로 부여된 행이 있어야 한다.
 *
 * <p>settlement 도메인의 동명 포트와 별개다 — 포트는 소비자가 소유하므로 finance도 자기 몫을
 * 직접 갖는다(정식 PagePermission 유스케이스가 생기기 전까지의 임시 구현, settlement 선례와 동일).
 */
public interface PagePermissionPort {

    boolean hasAccess(String pageCode, String userId, String role);

    /** {@code permission = 'EDITOR'} 등급인지 별도로 확인한다(쓰기 API용). ADMIN·MASTER는 여전히 무조건 통과. */
    boolean hasEditAccess(String pageCode, String userId, String role);
}
