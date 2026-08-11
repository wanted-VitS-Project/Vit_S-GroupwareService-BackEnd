package com.group3.vitamins.issue.presentation.api.response;

import com.group3.vitamins.issue.application.result.IssueResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "이슈 상세 응답")
public record IssueDetailResponse(

        @Schema(description = "이슈 번호", example = "101")
        Long issueId,

        @Schema(description = "동시 수정 검사용 현재 버전", example = "1")
        int version,

        @Schema(description = "소속 Step 번호", example = "12")
        Long stepId,

        @Schema(description = "제목", example = "제안서 1차 초안 작성")
        String title,

        @Schema(description = "내용. null 허용")
        String content,

        @Schema(description = "TODO · IN_PROGRESS · DONE", example = "TODO")
        String status,

        @Schema(description = "LOW · MEDIUM · HIGH", example = "HIGH")
        String priority,

        @Schema(description = "마감일. 미지정 시 null", example = "2026-08-05")
        LocalDate dueDate,

        @Schema(description = "DONE 완료 시각. 완료 상태가 아니면 null")
        LocalDateTime completedAt,

        @Schema(description = "담당자 목록")
        List<AssigneeResponse> assignees,

        @Schema(description = "관련 Block 목록")
        List<RelatedBlockResponse> relatedBlocks
) {

    public static IssueDetailResponse from(IssueResult result) {
        return new IssueDetailResponse(
                result.issueId(),
                result.version(),
                result.stepId(),
                result.title(),
                result.content(),
                result.status(),
                result.priority(),
                result.dueDate() == null ? null : result.dueDate().toLocalDate(),
                result.completedAt(),
                result.assignees().stream()
                        .map(assignee -> new AssigneeResponse(
                                assignee.userId(), assignee.name()))
                        .toList(),
                result.relatedBlocks().stream()
                        .map(block -> new RelatedBlockResponse(
                                block.blockId(), block.title(), block.type()))
                        .toList());
    }

    public record AssigneeResponse(
            @Schema(description = "담당자 사번", example = "EMP003")
            String userId,

            @Schema(description = "담당자 이름", example = "김용준")
            String name
    ) {
    }

    public record RelatedBlockResponse(
            @Schema(description = "관련 Block 번호", example = "15")
            Long blockId,

            @Schema(description = "관련 Block명", example = "제안서 작성 체크리스트")
            String title,

            @Schema(description = "Block 타입", example = "CHECKLIST")
            String type
    ) {
    }
}
