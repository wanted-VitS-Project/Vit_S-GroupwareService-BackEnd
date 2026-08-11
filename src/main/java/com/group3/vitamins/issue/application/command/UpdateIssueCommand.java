package com.group3.vitamins.issue.application.command;

import java.time.LocalDate;
import java.util.List;

public record UpdateIssueCommand(
        Long issueId,
        int version,
        PatchField<String> title,
        PatchField<String> content,
        PatchField<LocalDate> dueDate,
        PatchField<String> priority,
        PatchField<List<String>> assigneeIds,
        PatchField<List<Long>> blockIds,
        String requesterUserId,
        String role
) {
}
