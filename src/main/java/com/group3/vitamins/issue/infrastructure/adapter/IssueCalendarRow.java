package com.group3.vitamins.issue.infrastructure.adapter;

import java.time.LocalDateTime;

public record IssueCalendarRow(
        Long issueId,
        int version,
        String title,
        String status,
        String priority,
        LocalDateTime dueDate,
        Long stepId,
        String stepName,
        Long projectId,
        String projectName
) {
}
