package com.group3.vitamins.issue.presentation.api.request;

import com.group3.vitamins.issue.application.command.ChangeIssueStatusCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "이슈 상태 변경 요청")
public record IssueStatusChangeRequest(

        @NotBlank(message = "ISS_STATUS_REQUIRED|상태가 전달되지 않았습니다.")
        @Schema(description = "TODO · IN_PROGRESS · DONE", example = "DONE",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String status,

        @NotNull(message = "ISS_INVALID_REQUEST|버전은 필수입니다.")
        @Positive(message = "ISS_INVALID_REQUEST|버전은 1 이상이어야 합니다.")
        @Schema(description = "조회 응답에서 받은 Issue 버전", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer version
) {

    public ChangeIssueStatusCommand toCommand(Long issueId, String requesterUserId, String role) {
        return new ChangeIssueStatusCommand(issueId, status, version, requesterUserId, role);
    }
}
