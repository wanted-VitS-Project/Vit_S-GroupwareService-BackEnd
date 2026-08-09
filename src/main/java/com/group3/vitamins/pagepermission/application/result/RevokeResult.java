package com.group3.vitamins.pagepermission.application.result;

/**
 * 페이지 권한 회수(§5) 결과. 명시적 부여 기록만 회수되며, 대상이 ADMIN·MASTER 면 전역 권한으로 열람이 계속된다
 * (stillAccessible=true, accessSource=GLOBAL_ROLE). 아니면 stillAccessible=false, accessSource=null.
 */
public record RevokeResult(
        String pageCode,
        String userId,
        boolean stillAccessible,
        String accessSource
) {
}
