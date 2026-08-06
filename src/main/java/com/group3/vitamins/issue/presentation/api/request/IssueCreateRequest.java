package com.group3.vitamins.issue.presentation.api.request;

import com.group3.vitamins.issue.application.command.CreateIssueCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record IssueCreateRequest(

        @Schema(description = "제목. 공백 제외 필수, 최대 200자", example = "제안서 1차 초안 작성")
        String title,

        @Schema(description = "내용. null 허용", example = "공고 요구사항을 기준으로 제안서 초안을 작성한다.")
        String content,

        @Schema(description = "마감 일시", example = "2026-08-07T18:00:00")
        LocalDateTime dueDate,

        @Schema(description = "TODO · IN_PROGRESS · DONE. 기본 TODO", example = "TODO")
        String status,

        @Schema(description = "LOW · MEDIUM · HIGH", example = "HIGH")
        String priority,

        @Schema(description = "담당자 사번 목록. 생략 시 빈 목록")
        List<String> assigneeIds,

        @Schema(description = "관련 Block 번호 목록. 생략 시 빈 목록")
        List<Long> blockIds
) {

    public CreateIssueCommand toCommand(Long stepId, String requesterUserId, String role) {
        return new CreateIssueCommand(
                stepId,
                title,
                content,
                dueDate,
                status,
                priority,
                assigneeIds,
                blockIds,
                requesterUserId,
                role);
    }
}
