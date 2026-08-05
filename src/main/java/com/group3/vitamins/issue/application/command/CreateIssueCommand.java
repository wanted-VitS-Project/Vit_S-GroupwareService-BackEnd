package com.group3.vitamins.issue.application.command;

import java.time.LocalDateTime;
import java.util.List;

public record CreateIssueCommand(
        Long stepId,
        String title,
        String content,
        LocalDateTime dueDate,
        String status,
        String priority,
        List<String> assigneeIds,
        List<Long> blockIds,
        String requesterUserId,
        String role
) {
}
