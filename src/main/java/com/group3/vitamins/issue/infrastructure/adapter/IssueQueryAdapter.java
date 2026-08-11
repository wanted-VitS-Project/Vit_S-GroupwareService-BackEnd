package com.group3.vitamins.issue.infrastructure.adapter;

import com.group3.vitamins.issue.application.port.IssueQueryPort;
import com.group3.vitamins.issue.application.result.IssueResult;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IssueQueryAdapter implements IssueQueryPort {

    private final IssueQueryMapper issueQueryMapper;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public Optional<BlockStepResult> findBlockStep(Long blockId) {
        return issueQueryMapper.findBlockStep(blockId, currentCompanyIdProvider.currentCompanyId())
                .map(row -> new BlockStepResult(row.blockId(), row.stepId()));
    }

    @Override
    public Optional<Long> findProjectId(Long stepId) {
        return issueQueryMapper.findProjectId(stepId, currentCompanyIdProvider.currentCompanyId());
    }

    @Override
    public List<StepSummaryResult> findStepsByProject(Long projectId) {
        return issueQueryMapper.findStepsByProject(projectId, currentCompanyIdProvider.currentCompanyId()).stream()
                .map(row -> new StepSummaryResult(row.stepId(), row.stepName()))
                .toList();
    }

    @Override
    public List<IssueResult> findIssuesByProject(Long projectId) {
        return issueQueryMapper.findIssuesByProject(projectId, currentCompanyIdProvider.currentCompanyId()).stream()
                .map(this::toResultWithoutRelations)
                .toList();
    }

    @Override
    public Optional<IssueResult> findIssue(Long issueId) {
        return issueQueryMapper.findIssue(issueId, currentCompanyIdProvider.currentCompanyId())
                .map(this::toResultWithoutRelations);
    }

    @Override
    public List<IssueResult> findIssues(Long stepId, Long blockId) {
        return issueQueryMapper.findIssues(stepId, blockId, currentCompanyIdProvider.currentCompanyId()).stream()
                .map(this::toResultWithoutRelations)
                .toList();
    }

    @Override
    public List<AssigneeResult> findAssignees(Collection<Long> issueIds) {
        if (issueIds.isEmpty()) {
            return List.of();
        }
        return issueQueryMapper.findAssignees(issueIds, currentCompanyIdProvider.currentCompanyId()).stream()
                .map(row -> new AssigneeResult(
                        row.issueId(),
                        row.userId(),
                        row.name(),
                        row.resignedAt()
                ))
                .toList();
    }

    @Override
    public List<RelatedBlockResult> findRelatedBlocks(Collection<Long> issueIds) {
        if (issueIds.isEmpty()) {
            return List.of();
        }
        return issueQueryMapper.findRelatedBlocks(issueIds, currentCompanyIdProvider.currentCompanyId()).stream()
                .map(row -> new RelatedBlockResult(
                        row.issueId(),
                        row.blockId(),
                        row.title(),
                        row.type()
                ))
                .toList();
    }

    @Override
    public List<LinkableBlockResult> findLinkableBlocks(Collection<Long> blockIds) {
        if (blockIds.isEmpty()) {
            return List.of();
        }
        return issueQueryMapper.findLinkableBlocks(blockIds, currentCompanyIdProvider.currentCompanyId()).stream()
                .map(row -> new LinkableBlockResult(row.blockId(), row.stepId(), row.title(), row.type()))
                .toList();
    }

    @Override
    public List<CalendarIssueResult> findMyCalendarIssues(String userId) {
        return issueQueryMapper.findMyCalendarIssues(userId, currentCompanyIdProvider.currentCompanyId()).stream()
                .map(row -> new CalendarIssueResult(
                        row.issueId(),
                        row.version(),
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
                row.version(),
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
