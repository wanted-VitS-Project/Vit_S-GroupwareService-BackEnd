package com.group3.vitamins.issue.application.query;

public record IssueDetailQuery(
        Long issueId,
        String requesterUserId,
        String role
) {
}
