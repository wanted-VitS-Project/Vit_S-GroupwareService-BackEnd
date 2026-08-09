package com.group3.vitamins.project.step.presentation.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.group3.vitamins.project.step.application.result.StepDetailResult;
import com.group3.vitamins.project.step.application.result.StepPerson;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "스텝 상세 조회 응답")
public record StepDetailResponse(

        @Schema(description = "스텝 ID", example = "10")
        Long stepId,

        @Schema(description = "소속 프로젝트 ID", example = "12")
        Long projectId,

        @Schema(description = "소속 스테이지 ID. 미소속이면 null", example = "7", nullable = true)
        Long stageId,

        @Schema(description = "스텝명", example = "제안서 작성")
        String name,

        @Schema(description = "스텝 상태", example = "IN_PROGRESS")
        String status,

        @Schema(description = "시작일", example = "2026-08-01")
        LocalDate startedOn,

        @Schema(description = "종료일", example = "2026-08-10")
        LocalDate endedOn,

        @Schema(description = "책임자. 작업자가 아니다. 미지정이면 null")
        StepPersonResponse owner,

        @Schema(description = "전체 이슈 수", example = "5")
        int totalIssueCount,

        @Schema(description = "완료 이슈 수", example = "2")
        int doneIssueCount,

        @Schema(description = "진행 중 이슈 수. 진행 전 = total - done - inProgress", example = "2")
        int inProgressIssueCount,

        @Schema(description = "스텝 진척률(%) = 완료 이슈 / 전체 이슈. 이슈가 0개면 응답에서 빠진다",
                example = "40")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer progressRate,

        @Schema(description = "완료자. 미완료면 null")
        StepPersonResponse completedBy,

        @Schema(description = "완료 시각. 미완료면 null", nullable = true)
        LocalDateTime completedAt,

        @Schema(description = "요청자의 스텝 권한", example = "EDITOR")
        String myPermission
) {

    /** 조회 결과를 응답으로 옮긴다. */
    public static StepDetailResponse from(StepDetailResult result) {
        return new StepDetailResponse(
                result.stepId(), result.projectId(), result.stageId(), result.name(),
                result.status(), result.startedOn(), result.endedOn(),
                toPerson(result.owner()),
                result.totalIssueCount(), result.doneIssueCount(),
                result.inProgressIssueCount(), result.progressRate(),
                toPerson(result.completedBy()), result.completedAt(), result.myPermission());
    }

    private static StepPersonResponse toPerson(StepPerson person) {
        return person == null ? null : new StepPersonResponse(person.userId(), person.name());
    }
}