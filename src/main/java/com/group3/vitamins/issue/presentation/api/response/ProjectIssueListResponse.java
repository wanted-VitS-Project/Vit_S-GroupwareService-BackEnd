package com.group3.vitamins.issue.presentation.api.response;

import com.group3.vitamins.issue.application.result.IssueProjectListResult;
import com.group3.vitamins.issue.application.result.IssueResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "프로젝트 단위 이슈 목록 조회 응답")
public record ProjectIssueListResponse(

        @Schema(description = "프로젝트 전체 이슈 진척도")
        ProgressResponse progress,

        @Schema(description = "Step별로 묶인 이슈 목록. sortOrder 오름차순, 이슈 없는 Step도 빈 배열로 포함")
        List<StepIssuesResponse> steps
) {

    public static ProjectIssueListResponse from(IssueProjectListResult result) {
        return new ProjectIssueListResponse(
                ProgressResponse.from(result.progress()),
                result.steps().stream()
                        .map(StepIssuesResponse::from)
                        .toList());
    }

    public record ProgressResponse(
            @Schema(description = "전체 이슈 수", example = "12")
            int totalIssueCount,

            @Schema(description = "완료된 이슈 수", example = "5")
            int doneIssueCount,

            @Schema(description = "진행 중인 이슈 수", example = "3")
            int inProgressIssueCount,

            @Schema(description = "완료율(%). 이슈가 0개면 null", example = "41")
            Integer progressRate
    ) {

        private static ProgressResponse from(IssueProjectListResult.ProgressResult result) {
            return new ProgressResponse(
                    result.totalIssueCount(),
                    result.doneIssueCount(),
                    result.inProgressIssueCount(),
                    result.progressRate()
            );
        }
    }

    public record StepIssuesResponse(
            @Schema(description = "Step ID", example = "10")
            Long stepId,

            @Schema(description = "Step 이름", example = "요구사항 정의")
            String stepName,

            @Schema(description = "해당 Step의 전체 이슈 수", example = "4")
            int totalIssueCount,

            @Schema(description = "해당 Step의 완료된 이슈 수", example = "2")
            int doneIssueCount,

            @Schema(description = "해당 Step의 진행 중인 이슈 수", example = "1")
            int inProgressIssueCount,

            @Schema(description = "해당 Step의 완료율(%). 이슈가 0개면 null", example = "50")
            Integer progressRate,

            @Schema(description = "해당 Step의 이슈 목록")
            List<IssueSummaryResponse> issues
    ) {

        private static StepIssuesResponse from(IssueProjectListResult.StepIssuesResult result) {
            return new StepIssuesResponse(
                    result.stepId(),
                    result.stepName(),
                    result.totalIssueCount(),
                    result.doneIssueCount(),
                    result.inProgressIssueCount(),
                    result.progressRate(),
                    result.issues().stream()
                            .map(IssueSummaryResponse::from)
                            .toList()
            );
        }
    }

    public record IssueSummaryResponse(
            @Schema(description = "이슈 ID", example = "101")
            Long issueId,

            @Schema(description = "이슈 제목", example = "경쟁사 제안서 벤치마킹")
            String title,

            @Schema(description = "TODO · IN_PROGRESS · DONE", example = "TODO")
            String status,

            @Schema(description = "LOW · MEDIUM · HIGH", example = "HIGH")
            String priority,

            @Schema(description = "마감일. 미지정 시 null", example = "2026-07-25")
            LocalDate dueDate,

            @Schema(description = "담당자 목록")
            List<AssigneeResponse> assignees,

            @Schema(description = "연결된 Block 목록")
            List<RelatedBlockResponse> relatedBlocks
    ) {

        private static IssueSummaryResponse from(IssueResult result) {
            return new IssueSummaryResponse(
                    result.issueId(),
                    result.title(),
                    result.status(),
                    result.priority(),
                    result.dueDate() == null ? null : result.dueDate().toLocalDate(),
                    result.assignees().stream()
                            .map(assignee -> new AssigneeResponse(
                                    assignee.userId(),
                                    assignee.name()
                            ))
                            .toList(),
                    result.relatedBlocks().stream()
                            .map(block -> new RelatedBlockResponse(
                                    block.blockId(),
                                    block.title(),
                                    block.type()
                            ))
                            .toList()
            );
        }
    }

    public record AssigneeResponse(
            @Schema(description = "담당자 사번", example = "EMP001")
            String userId,

            @Schema(description = "담당자 이름", example = "김용준")
            String name
    ) {
    }

    public record RelatedBlockResponse(
            @Schema(description = "Block ID", example = "15")
            Long blockId,

            @Schema(description = "Block 제목", example = "제안서 작성 체크리스트")
            String title,

            @Schema(description = "Block 유형", example = "CHECKLIST")
            String type
    ) {
    }
}
