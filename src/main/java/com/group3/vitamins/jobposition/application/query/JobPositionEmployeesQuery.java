package com.group3.vitamins.jobposition.application.query;

/**
 * 직급별 사원 목록 조회 쿼리 (`.ai/api/job-position.md` §5). ADMIN 전용이라 role 로 권한을 판정한다.
 * 페이징·검색 파라미터는 받지 않는다(§5 정책).
 */
public record JobPositionEmployeesQuery(
        Long jobPositionId,
        String role
) {
}
