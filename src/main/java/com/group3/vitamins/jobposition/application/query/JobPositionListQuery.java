package com.group3.vitamins.jobposition.application.query;

/**
 * 직급 목록 조회 쿼리. 목록도 ADMIN 전용이라 role 로 권한을 판정한다 (`job-position.md` §1).
 * 페이징·검색·정렬 파라미터는 받지 않는다.
 */
public record JobPositionListQuery(
        String role
) {
}
