package com.group3.vitamins.issue.application.command;

public record ChangeIssueStatusCommand(
        Long issueId,
        String status,
        String requesterUserId,
        String role
) {
}
