package com.group3.vitamins.jobposition.presentation.api.response;

import com.group3.vitamins.jobposition.application.result.JobPositionResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "직급 생성·수정 응답")
public record JobPositionDetailResponse(

        @Schema(description = "직급 번호", example = "2")
        Long jobPositionId,

        @Schema(description = "직급명", example = "대리")
        String name,

        @Schema(description = "정렬 순서", example = "2")
        int sortOrder,

        @Schema(description = "사용 인원. 생성 직후이면 0", example = "0")
        int employeeCount
) {

    /** 생성·수정 결과를 응답으로 옮긴다. */
    public static JobPositionDetailResponse from(JobPositionResult result) {
        return new JobPositionDetailResponse(
                result.jobPositionId(),
                result.name(),
                result.sortOrder(),
                result.employeeCount()
        );
    }
}
