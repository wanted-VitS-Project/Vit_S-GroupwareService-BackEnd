package com.group3.vitamins.issue.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record IssueResult(
        Long issueId,
        int version,
        Long stepId,
        String title,
        String content,
        String status,
        String priority,
        LocalDateTime dueDate,
        LocalDateTime completedAt,
        List<AssigneeResult> assignees,
        List<BlockResult> relatedBlocks
) {

    public record AssigneeResult(String userId, String name, LocalDate resignedAt) {
    }

    public record BlockResult(Long blockId, String title, String type) {
    }
}
