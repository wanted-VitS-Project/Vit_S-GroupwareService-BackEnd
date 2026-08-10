package com.group3.vitamins.issue.presentation.api.response;

import com.group3.vitamins.issue.application.result.IssueCalendarResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "담당 이슈 캘린더 조회 응답")
public record IssueCalendarResponse(

        @Schema(description = "조회된 이슈 목록")
        List<IssueCalendarItemResponse> issues
) {

    public static IssueCalendarResponse from(IssueCalendarResult result) {
        return new IssueCalendarResponse(result.issues().stream()
                .map(IssueCalendarItemResponse::from)
                .toList());
    }

    public record IssueCalendarItemResponse(
            @Schema(description = "이슈 ID", example = "101")
            Long issueId,

            @Schema(description = "동시 수정 검사용 현재 버전", example = "1")
            int version,

            @Schema(description = "이슈 제목", example = "제안서 1차 초안 작성")
            String title,

            @Schema(description = "TODO · IN_PROGRESS (DONE은 반환하지 않음)", example = "IN_PROGRESS")
            String status,

            @Schema(description = "LOW · MEDIUM · HIGH", example = "HIGH")
            String priority,

            @Schema(description = "마감일", example = "2026-08-11")
            LocalDate dueDate,

            @Schema(description = "소속 Step ID", example = "10")
            Long stepId,

            @Schema(description = "소속 Step명", example = "입찰 진행")
            String stepName,

            @Schema(description = "소속 Project ID", example = "3")
            Long projectId,

            @Schema(description = "소속 Project명", example = "OO시 스마트도로 구축")
            String projectName
    ) {

        private static IssueCalendarItemResponse from(IssueCalendarResult.CalendarIssueResult result) {
            return new IssueCalendarItemResponse(
                    result.issueId(),
                    result.version(),
                    result.title(),
                    result.status(),
                    result.priority(),
                    result.dueDate() == null ? null : result.dueDate().toLocalDate(),
                    result.stepId(),
                    result.stepName(),
                    result.projectId(),
                    result.projectName()
            );
        }
    }
}
