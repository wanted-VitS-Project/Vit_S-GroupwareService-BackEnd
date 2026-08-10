package com.group3.vitamins.issue.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.issue.application.port.IssueQueryPort;
import com.group3.vitamins.issue.application.port.IssueStepAccessPort;
import com.group3.vitamins.issue.application.query.IssueCalendarQuery;
import com.group3.vitamins.issue.application.query.IssueDetailQuery;
import com.group3.vitamins.issue.application.query.IssueListQuery;
import com.group3.vitamins.issue.application.query.IssueProjectListQuery;
import com.group3.vitamins.issue.application.result.IssueCalendarResult;
import com.group3.vitamins.issue.application.result.IssueListResult;
import com.group3.vitamins.issue.application.result.IssueProjectListResult;
import com.group3.vitamins.issue.application.result.IssueResult;
import com.group3.vitamins.issue.application.usecase.IssueQueryUseCase;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import com.group3.vitamins.project.block.domain.exception.BlockErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueQueryService implements IssueQueryUseCase {

    private final IssueStepAccessPort issueStepAccessPort;
    private final IssueQueryPort issueQueryPort;

    @Override
    public IssueListResult getIssues(IssueListQuery query) {
        issueStepAccessPort.requireAccess(query.stepId(), query.requesterUserId(), query.role());
        validateBlockFilter(query.stepId(), query.blockId());

        List<IssueResult> issues = issueQueryPort.findIssues(query.stepId(), query.blockId());
        if (issues.isEmpty()) {
            return new IssueListResult(List.of());
        }

        List<Long> issueIds = issues.stream()
                .map(IssueResult::issueId)
                .toList();
        Map<Long, List<IssueQueryPort.AssigneeResult>> assigneesByIssueId =
                issueQueryPort.findAssignees(issueIds).stream()
                        .collect(Collectors.groupingBy(IssueQueryPort.AssigneeResult::issueId));
        Map<Long, List<IssueQueryPort.RelatedBlockResult>> blocksByIssueId =
                issueQueryPort.findRelatedBlocks(issueIds).stream()
                        .collect(Collectors.groupingBy(IssueQueryPort.RelatedBlockResult::issueId));

        return new IssueListResult(issues.stream()
                .map(issue -> withRelations(issue, assigneesByIssueId, blocksByIssueId))
                .toList());
    }

    @Override
    public IssueResult getIssue(IssueDetailQuery query) {
        IssueResult issue = issueQueryPort.findIssue(query.issueId())
                .orElseThrow(() -> new NotFoundException(IssueErrorCode.ISS_NOT_FOUND));

        issueStepAccessPort.requireIssueAccess(issue.stepId(), query.requesterUserId(), query.role());

        Map<Long, List<IssueQueryPort.AssigneeResult>> assigneesByIssueId =
                issueQueryPort.findAssignees(List.of(issue.issueId())).stream()
                        .collect(Collectors.groupingBy(IssueQueryPort.AssigneeResult::issueId));
        Map<Long, List<IssueQueryPort.RelatedBlockResult>> blocksByIssueId =
                issueQueryPort.findRelatedBlocks(List.of(issue.issueId())).stream()
                        .collect(Collectors.groupingBy(IssueQueryPort.RelatedBlockResult::issueId));

        return withRelations(issue, assigneesByIssueId, blocksByIssueId);
    }

    @Override
    public IssueCalendarResult getMyCalendarIssues(IssueCalendarQuery query) {
        List<IssueQueryPort.CalendarIssueResult> rows =
                issueQueryPort.findMyCalendarIssues(query.requesterUserId());

        return new IssueCalendarResult(rows.stream()
                .map(row -> new IssueCalendarResult.CalendarIssueResult(
                        row.issueId(),
                        row.title(),
                        row.status(),
                        row.priority(),
                        row.dueDate(),
                        row.stepId(),
                        row.stepName(),
                        row.projectId(),
                        row.projectName()
                ))
                .toList());
    }

    @Override
    public IssueProjectListResult getIssuesByProject(IssueProjectListQuery query) {
        issueStepAccessPort.requireProjectAccess(query.projectId(), query.requesterUserId(), query.role());

        List<IssueQueryPort.StepSummaryResult> steps = issueQueryPort.findStepsByProject(query.projectId());
        List<IssueResult> issues = withRelationsBatch(issueQueryPort.findIssuesByProject(query.projectId()));

        Map<Long, List<IssueResult>> issuesByStepId = issues.stream()
                .collect(Collectors.groupingBy(IssueResult::stepId));

        List<IssueProjectListResult.StepIssuesResult> stepResults = steps.stream()
                .map(step -> {
                    List<IssueResult> stepIssues = issuesByStepId.getOrDefault(step.stepId(), List.of());
                    IssueProjectListResult.ProgressResult stepProgress = progressOf(stepIssues);
                    return new IssueProjectListResult.StepIssuesResult(
                            step.stepId(),
                            step.stepName(),
                            stepProgress.totalIssueCount(),
                            stepProgress.doneIssueCount(),
                            stepProgress.inProgressIssueCount(),
                            stepProgress.progressRate(),
                            stepIssues);
                })
                .toList();

        return new IssueProjectListResult(progressOf(issues), stepResults);
    }

    private List<IssueResult> withRelationsBatch(List<IssueResult> issues) {
        if (issues.isEmpty()) {
            return List.of();
        }

        List<Long> issueIds = issues.stream()
                .map(IssueResult::issueId)
                .toList();
        Map<Long, List<IssueQueryPort.AssigneeResult>> assigneesByIssueId =
                issueQueryPort.findAssignees(issueIds).stream()
                        .collect(Collectors.groupingBy(IssueQueryPort.AssigneeResult::issueId));
        Map<Long, List<IssueQueryPort.RelatedBlockResult>> blocksByIssueId =
                issueQueryPort.findRelatedBlocks(issueIds).stream()
                        .collect(Collectors.groupingBy(IssueQueryPort.RelatedBlockResult::issueId));

        return issues.stream()
                .map(issue -> withRelations(issue, assigneesByIssueId, blocksByIssueId))
                .toList();
    }

    /** 완료율은 totalIssueCount가 0이면 null — 0/0 나눗셈을 응답에 노출하지 않는다(Step 진척도와 동일 규칙). */
    private IssueProjectListResult.ProgressResult progressOf(List<IssueResult> issues) {
        int total = issues.size();
        int done = (int) issues.stream().filter(issue -> "DONE".equals(issue.status())).count();
        int inProgress = (int) issues.stream().filter(issue -> "IN_PROGRESS".equals(issue.status())).count();
        Integer progressRate = total == 0 ? null : done * 100 / total;
        return new IssueProjectListResult.ProgressResult(total, done, inProgress, progressRate);
    }

    private void validateBlockFilter(Long stepId, Long blockId) {
        if (blockId == null) {
            return;
        }

        IssueQueryPort.BlockStepResult block = issueQueryPort.findBlockStep(blockId)
                .orElseThrow(() -> new NotFoundException(BlockErrorCode.BLOCK_NOT_FOUND));
        if (!stepId.equals(block.stepId())) {
            throw new ValidationException(IssueErrorCode.ISS_BLOCK_STEP_MISMATCH);
        }
    }

    private IssueResult withRelations(
            IssueResult issue,
            Map<Long, List<IssueQueryPort.AssigneeResult>> assigneesByIssueId,
            Map<Long, List<IssueQueryPort.RelatedBlockResult>> blocksByIssueId
    ) {
        List<IssueResult.AssigneeResult> assignees = assigneesByIssueId
                .getOrDefault(issue.issueId(), List.of()).stream()
                .map(row -> new IssueResult.AssigneeResult(
                        row.userId(),
                        row.name(),
                        row.resignedAt()
                ))
                .toList();
        List<IssueResult.BlockResult> relatedBlocks = blocksByIssueId
                .getOrDefault(issue.issueId(), List.of()).stream()
                .map(row -> new IssueResult.BlockResult(
                        row.blockId(),
                        row.title(),
                        row.type()
                ))
                .toList();

        return new IssueResult(
                issue.issueId(),
                issue.stepId(),
                issue.title(),
                issue.content(),
                issue.status(),
                issue.priority(),
                issue.dueDate(),
                issue.completedAt(),
                assignees,
                relatedBlocks
        );
    }
}
