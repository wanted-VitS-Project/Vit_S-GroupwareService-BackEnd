package com.group3.vitamins.jobposition.application.result;

import java.util.List;

/**
 * 직급별 사원 목록 조회 결과 (`.ai/api/job-position.md` §5).
 *
 * <p>직급은 목록 전체가 같으므로 {@code jobPositionId}·{@code jobPositionName} 을 항목마다 반복하지 않고
 * 결과 최상위에 한 번만 둔다.
 */
public record JobPositionEmployeesResult(
        Long jobPositionId,
        String jobPositionName,
        List<Employee> content
) {

    /** 직급에 속한 사원 한 명. */
    public record Employee(
            String userId,
            String name,
            String departmentName,
            String departmentPath
    ) {
    }
}
