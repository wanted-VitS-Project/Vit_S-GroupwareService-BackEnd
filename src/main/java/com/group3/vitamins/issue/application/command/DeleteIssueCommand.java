package com.group3.vitamins.issue.application.command;

public record DeleteIssueCommand(
        Long issueId,
        String requesterUserId,
        String role
) {
}
