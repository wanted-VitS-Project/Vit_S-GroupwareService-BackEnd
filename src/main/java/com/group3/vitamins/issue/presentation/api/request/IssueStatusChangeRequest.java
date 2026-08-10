package com.group3.vitamins.issue.presentation.api.request;

import com.group3.vitamins.issue.application.command.ChangeIssueStatusCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이슈 상태 변경 요청")
public record IssueStatusChangeRequest(

        @Schema(description = "TODO · IN_PROGRESS · DONE", example = "DONE",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String status,

        @Schema(description = "조회 응답에서 받은 Issue 버전", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer version
) {

    public ChangeIssueStatusCommand toCommand(Long issueId, String requesterUserId, String role) {
        return new ChangeIssueStatusCommand(issueId, status, version, requesterUserId, role);
    }
}
