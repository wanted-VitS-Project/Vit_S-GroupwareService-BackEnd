package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.EmployeeSearchRow;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사원 검색 결과 항목 (결재자 후보)")
public record EmployeeSearchResponse(

        @Schema(description = "사번", example = "EMP001")
        String userId,

        @Schema(description = "이름", example = "김민준")
        String name,

        @Schema(description = "부서명 (동명이인 구분용, 미배정이면 null)", example = "개발팀")
        String department,

        @Schema(description = "직급명 (동명이인 구분용, 미배정이면 null)", example = "대리")
        String position
) {

    /** 조회 결과를 응답 항목으로 옮긴다. */
    public static EmployeeSearchResponse from(EmployeeSearchRow row) {
        return new EmployeeSearchResponse(
                row.userId(),
                row.name(),
                row.department(),
                row.position()
        );
    }
}
