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

    Optional<Long> findProjectId(Long stepId);

    /** 프로젝트에 속한, 삭제되지 않은 Step을 sortOrder 오름차순으로 돌려준다. 이슈 유무와 무관하게 전부 포함한다. */
    List<StepSummaryResult> findStepsByProject(Long projectId);

    /** 프로젝트에 속한 모든 Step(삭제된 Step 제외)의 이슈를 Step sortOrder, 이슈ID 역순으로 돌려준다. */
    List<IssueResult> findIssuesByProject(Long projectId);

    record BlockStepResult(Long blockId, Long stepId) {
    }

    record StepSummaryResult(Long stepId, String stepName) {
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
