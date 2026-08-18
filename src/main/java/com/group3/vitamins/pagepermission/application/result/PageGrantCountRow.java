package com.group3.vitamins.pagepermission.application.result;

/** §2 페이지별 부여 인원 수 집계 행(회사 MEMBER 기준). 부여 기록이 없는 페이지는 결과에 빠진다(→ 0). */
public record PageGrantCountRow(
        String pageCode,
        long grantedCount
) {
}
