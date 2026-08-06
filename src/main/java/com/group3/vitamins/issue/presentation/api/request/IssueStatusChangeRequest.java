package com.group3.vitamins.issue.presentation.api.request;

import com.group3.vitamins.issue.application.command.ChangeIssueStatusCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이슈 상태 변경 요청")
public record IssueStatusChangeRequest(

        @Schema(description = "TODO · IN_PROGRESS · DONE", example = "DONE")
        String status
) {

    public ChangeIssueStatusCommand toCommand(Long issueId, String requesterUserId, String role) {
        return new ChangeIssueStatusCommand(issueId, status, requesterUserId, role);
    }
}
