package com.group3.vitamins.issue.infrastructure.adapter;

import java.time.LocalDateTime;

public record IssueRow(
        Long issueId,
        Long stepId,
        String title,
        String content,
        String status,
        String priority,
        LocalDateTime dueDate,
        LocalDateTime completedAt
) {
}
