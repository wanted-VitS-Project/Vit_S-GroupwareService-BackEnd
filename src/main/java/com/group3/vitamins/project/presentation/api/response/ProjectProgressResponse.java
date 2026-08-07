package com.group3.vitamins.project.presentation.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.group3.vitamins.project.application.result.ProjectProgressResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 진척률 조회 응답")
public record ProjectProgressResponse(

        @Schema(description = "프로젝트 ID", example = "12")
        Long projectId,

        @Schema(description = "전체 스텝 수", example = "5")
        int totalStepCount,

        @Schema(description = "완료 스텝 수", example = "2")
        int doneStepCount,

        @Schema(description = "진척률(%). 스텝이 0개면 응답에서 빠진다", example = "40")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer progressRate
) {

    /** 조회 결과를 응답으로 옮긴다. */
    public static ProjectProgressResponse from(ProjectProgressResult result) {
        return new ProjectProgressResponse(
                result.projectId(), result.totalStepCount(),
                result.doneStepCount(), result.progressRate());
    }
}