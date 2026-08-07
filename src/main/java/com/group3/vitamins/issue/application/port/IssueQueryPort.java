package com.group3.vitamins.issue.application.port;

import com.group3.vitamins.issue.application.result.IssueResult;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IssueQueryPort {

    Optional<BlockStepResult> findBlockStep(Long blockId);

    Optional<IssueResult> findIssue(Long issueId);

    List<IssueResult> findIssues(Long stepId, Long blockId);

    List<AssigneeResult> findAssignees(Collection<Long> issueIds);

    List<RelatedBlockResult> findRelatedBlocks(Collection<Long> issueIds);

    List<CalendarIssueResult> findMyCalendarIssues(String userId);

    record BlockStepResult(Long blockId, Long stepId) {
    }

    record AssigneeResult(Long issueId, String userId, String name) {
    }

    record RelatedBlockResult(Long issueId, Long blockId, String title, String type) {
    }

    record CalendarIssueResult(
            Long issueId,
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
