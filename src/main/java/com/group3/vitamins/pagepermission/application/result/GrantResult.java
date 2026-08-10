package com.group3.vitamins.pagepermission.application.result;

/** 페이지 권한 부여(§4) 결과. requested = 신규(granted) + 등급변경(updated) + 변화없음(unchanged). */
public record GrantResult(
        String pageCode,
        int requestedCount,
        int grantedCount,
        int updatedCount,
        int unchangedCount
) {
}
