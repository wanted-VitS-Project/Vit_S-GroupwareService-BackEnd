package com.group3.vitamins.jobposition.presentation.api.response;

import com.group3.vitamins.jobposition.application.result.JobPositionResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "직급 목록 항목")
public record JobPositionResponse(

        @Schema(description = "직급 번호", example = "1")
        Long jobPositionId,

        @Schema(description = "직급명", example = "사원")
        String name,

        @Schema(description = "정렬 순서", example = "1")
        int sortOrder,

        @Schema(description = "사용 인원 (시스템 계정·퇴사자·삭제 사원 제외)", example = "14")
        int employeeCount
) {

    /** 조회 결과를 목록 응답 항목으로 옮긴다. */
    public static JobPositionResponse from(JobPositionResult result) {
        return new JobPositionResponse(
                result.jobPositionId(),
                result.name(),
                result.sortOrder(),
                result.employeeCount()
        );
    }
}
