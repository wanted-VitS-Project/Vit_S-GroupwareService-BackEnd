package com.group3.vitamins.project.step.presentation.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.group3.vitamins.project.step.application.result.StepSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "스텝 목록 조회 응답")
public record StepListResponse(

        @Schema(description = "스텝 목록 (sortOrder 오름차순)")
        List<StepItemResponse> steps
) {

    /** 조회 결과를 응답으로 옮긴다. */
    public static StepListResponse from(List<StepSummary> summaries) {
        return new StepListResponse(summaries.stream()
                .map(StepItemResponse::from)
                .toList());
    }

    @Schema(description = "스텝 요약")
    public record StepItemResponse(

            @Schema(description = "스텝 ID", example = "10")
            Long stepId,

            @Schema(description = "소속 스테이지 ID. 미소속이면 null", example = "7")
            Long stageId,

            @Schema(description = "스텝명", example = "제안서 작성")
            String name,

            @Schema(description = "스텝 상태", example = "IN_PROGRESS")
            String status,

            @Schema(description = "정렬 순서", example = "1")
            int sortOrder,

            @Schema(description = "시작일", example = "2026-08-01")
            LocalDate startedOn,

            @Schema(description = "종료일", example = "2026-08-10")
            LocalDate endedOn,

            @Schema(description = "책임자. 미지정이면 null")
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

            @Schema(description = "요청자의 스텝 권한", example = "EDITOR")
            String myPermission
    ) {

        static StepItemResponse from(StepSummary summary) {
            StepPersonResponse owner = summary.owner() == null
                    ? null
                    : new StepPersonResponse(summary.owner().userId(), summary.owner().name());

            return new StepItemResponse(
                    summary.stepId(), summary.stageId(), summary.name(), summary.status(),
                    summary.sortOrder(), summary.startedOn(), summary.endedOn(), owner,
                    summary.totalIssueCount(), summary.doneIssueCount(),
                    summary.inProgressIssueCount(), summary.progressRate(),
                    summary.myPermission());
        }
    }
}