package com.group3.vitamins.issue.infrastructure.adapter;

public record IssueAssigneeRow(
        Long issueId,
        String userId,
        String name
) {
}
