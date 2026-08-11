package com.group3.vitamins.issue.application.result;

import java.time.LocalDateTime;

public record IssueStatusResult(
        Long issueId,
        int version,
        String status,
        LocalDateTime completedAt,
        LocalDateTime updatedAt
) {
}
