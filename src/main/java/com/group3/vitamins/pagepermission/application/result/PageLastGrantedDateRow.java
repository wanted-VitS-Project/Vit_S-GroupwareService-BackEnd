package com.group3.vitamins.pagepermission.application.result;

import java.time.LocalDate;

/** §2 페이지별 마지막 부여 수정일(yyyy-MM-dd, 회사 사원 기준). 부여 기록이 없는 페이지는 결과에 빠진다(→ null). */
public record PageLastGrantedDateRow(
        String pageCode,
        LocalDate lastGrantedDate
) {
}
