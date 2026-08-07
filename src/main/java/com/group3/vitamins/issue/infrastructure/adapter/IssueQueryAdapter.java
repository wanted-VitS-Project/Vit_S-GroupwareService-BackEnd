package com.group3.vitamins.issue.infrastructure.adapter;

import com.group3.vitamins.issue.application.port.IssueQueryPort;
import com.group3.vitamins.issue.application.result.IssueResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IssueQueryAdapter implements IssueQueryPort {

    private final IssueQueryMapper issueQueryMapper;

    @Override
    public Optional<BlockStepResult> findBlockStep(Long blockId) {
        return issueQueryMapper.findBlockStep(blockId)
                .map(row -> new BlockStepResult(row.blockId(), row.stepId()));
    }

    @Override
    public Optional<IssueResult> findIssue(Long issueId) {
        return issueQueryMapper.findIssue(issueId)
                .map(this::toResultWithoutRelations);
    }

    @Override
    public List<IssueResult> findIssues(Long stepId, Long blockId) {
        return issueQueryMapper.findIssues(stepId, blockId).stream()
                .map(this::toResultWithoutRelations)
                .toList();
    }

    @Override
    public List<AssigneeResult> findAssignees(Collection<Long> issueIds) {
        if (issueIds.isEmpty()) {
            return List.of();
        }
        return issueQueryMapper.findAssignees(issueIds).stream()
                .map(row -> new AssigneeResult(
                        row.issueId(),
                        row.userId(),
                        row.name()
                ))
                .toList();
    }

    @Override
    public List<RelatedBlockResult> findRelatedBlocks(Collection<Long> issueIds) {
        if (issueIds.isEmpty()) {
            return List.of();
        }
        return issueQueryMapper.findRelatedBlocks(issueIds).stream()
                .map(row -> new RelatedBlockResult(
                        row.issueId(),
                        row.blockId(),
                        row.title(),
                        row.type()
                ))
                .toList();
    }

    @Override
    public List<CalendarIssueResult> findMyCalendarIssues(String userId) {
        return issueQueryMapper.findMyCalendarIssues(userId).stream()
                .map(row -> new CalendarIssueResult(
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
                .toList();
    }

    private IssueResult toResultWithoutRelations(IssueRow row) {
        return new IssueResult(
                row.issueId(),
                row.stepId(),
                row.title(),
                row.content(),
                row.status(),
                row.priority(),
                row.dueDate(),
                row.completedAt(),
                List.of(),
                List.of()
        );
    }
}
