package com.group3.vitamins.issue.application.query;

public record IssueProjectListQuery(
        Long projectId,
        String requesterUserId,
        String role
) {
}
