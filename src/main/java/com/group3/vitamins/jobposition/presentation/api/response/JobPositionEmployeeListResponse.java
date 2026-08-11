package com.group3.vitamins.jobposition.presentation.api.response;

import com.group3.vitamins.jobposition.application.result.JobPositionEmployeesResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 직급별 사원 목록 응답 (`.ai/api/job-position.md` §5).
 *
 * <p>직급은 목록 전체 공통이라 항목마다 반복하지 않고 최상위에 한 번만 둔다.
 * ⛔ 페이징이 없다 — 직급 하나의 인원 규모라 전건을 {@code content} 로 내린다(§1 과 동일 정책).
 */
@Schema(name = "JobPositionEmployeeList", description = "직급별 사원 목록")
public record JobPositionEmployeeListResponse(

        @Schema(description = "조회한 직급 번호", example = "1")
        Long jobPositionId,

        @Schema(description = "조회한 직급명", example = "사원")
        String jobPositionName,

        @Schema(description = "사원 목록 (이름 오름차순, 동명이인은 사번 오름차순)")
        List<Item> content
) {

    @Schema(name = "JobPositionEmployeeListResponseItem", description = "직급에 속한 사원")
    public record Item(

            @Schema(description = "사번", example = "EMP001")
            String userId,

            @Schema(description = "이름", example = "김철수")
            String name,

            @Schema(description = "부서명 (부서 미배정이면 null)", example = "개발팀")
            String departmentName,

            @Schema(description = "부서 경로 (부서 미배정이면 null)", example = "본사 / 개발팀")
            String departmentPath
    ) {
    }

    /** 조회 결과를 응답 봉투의 data 로 감싼다. */
    public static JobPositionEmployeeListResponse from(JobPositionEmployeesResult result) {
        return new JobPositionEmployeeListResponse(
                result.jobPositionId(),
                result.jobPositionName(),
                result.content().stream()
                        .map(e -> new Item(e.userId(), e.name(), e.departmentName(), e.departmentPath()))
                        .toList());
    }
}
