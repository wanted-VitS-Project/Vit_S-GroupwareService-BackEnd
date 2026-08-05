package com.group3.vitamins.issue.application.query;

public record IssueListQuery(
        Long stepId,
        Long blockId,
        String requesterUserId,
        String role
) {
}
