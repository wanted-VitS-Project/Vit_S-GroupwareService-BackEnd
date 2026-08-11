package com.group3.vitamins.issue.infrastructure.adapter;

import java.time.LocalDate;

public record IssueAssigneeRow(
        Long issueId,
        String userId,
        String name,
        LocalDate resignedAt
) {
}
