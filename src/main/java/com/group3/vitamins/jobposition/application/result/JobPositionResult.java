package com.group3.vitamins.jobposition.application.result;

import com.group3.vitamins.jobposition.domain.model.JobPosition;

/**
 * 직급 서비스 출력. 목록·생성·수정이 모두 같은 구조를 반환한다 (`job-position.md`).
 * {@code employeeCount} 는 {@code employee} 를 가로지르는 집계라 도메인 객체에 없고 서비스가 채운다.
 */
public record JobPositionResult(
        Long jobPositionId,
        String name,
        int sortOrder,
        int employeeCount
) {

    /** 도메인 객체와 사용 인원으로 결과를 만든다. */
    public static JobPositionResult of(JobPosition jobPosition, int employeeCount) {
        return new JobPositionResult(
                jobPosition.getJobPositionId(),
                jobPosition.getName(),
                jobPosition.getSortOrder(),
                employeeCount
        );
    }
}
