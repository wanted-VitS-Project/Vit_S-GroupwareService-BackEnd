package com.group3.vitamins.project.step.presentation.api.response;

import com.group3.vitamins.project.step.application.result.StepResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "스텝 생성 응답")
public record StepCreateResponse(

        @Schema(description = "생성된 스텝 ID", example = "10")
        Long stepId,

        @Schema(description = "소속 프로젝트 ID", example = "12")
        Long projectId,

        @Schema(description = "소속 스테이지 ID. 미소속이면 null", example = "7", nullable = true)
        Long stageId,

        @Schema(description = "스텝명", example = "제안서 작성")
        String name,

        @Schema(description = "스텝 상태. 생성 직후는 항상 NOT_STARTED", example = "NOT_STARTED")
        String status,

        @Schema(description = "정렬 순서. 프로젝트 전체 기준 max+1", example = "1")
        int sortOrder,

        @Schema(description = "시작일", example = "2026-08-01")
        LocalDate startedOn,

        @Schema(description = "종료일", example = "2026-08-10")
        LocalDate endedOn,

        @Schema(description = "책임자. ownerUserId 를 안 보냈으면 null")
        StepPersonResponse owner,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {

    /** 생성 결과를 응답으로 옮긴다. */
    public static StepCreateResponse from(StepResult result) {
        StepPersonResponse owner = result.owner() == null
                ? null
                : new StepPersonResponse(result.owner().userId(), result.owner().name());

        return new StepCreateResponse(
                result.stepId(), result.projectId(), result.stageId(), result.name(),
                result.status(), result.sortOrder(), result.startedOn(), result.endedOn(),
                owner, result.createdAt());
    }
}