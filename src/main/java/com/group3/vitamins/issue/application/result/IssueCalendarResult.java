package com.group3.vitamins.issue.application.result;

import java.time.LocalDateTime;
import java.util.List;

public record IssueCalendarResult(
        List<CalendarIssueResult> issues
) {

    public record CalendarIssueResult(
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
}
