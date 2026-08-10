package com.group3.vitamins.issue.presentation.api.response;

import com.group3.vitamins.issue.application.result.IssueListResult;
import com.group3.vitamins.issue.application.result.IssueResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "이슈 목록 조회 응답")
public record IssueListResponse(

        @Schema(description = "조회된 이슈 목록")
        List<IssueSummaryResponse> issues
) {

    public static IssueListResponse from(IssueListResult result) {
        return new IssueListResponse(result.issues().stream()
                .map(IssueSummaryResponse::from)
                .toList());
    }

    public record IssueSummaryResponse(
            @Schema(description = "이슈 ID", example = "101")
            Long issueId,

            @Schema(description = "동시 수정 검사용 현재 버전", example = "1")
            int version,

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
                    result.version(),
                    result.title(),
                    result.status(),
                    result.priority(),
                    result.dueDate() == null ? null : result.dueDate().toLocalDate(),
                    result.assignees().stream()
                            .map(assignee -> new AssigneeResponse(
                                    assignee.userId(),
                                    assignee.name(),
                                    assignee.resignedAt()
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
            String name,

            @Schema(description = "퇴사일. 재직 중이면 null", example = "2026-08-01", nullable = true)
            LocalDate resignedAt
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
