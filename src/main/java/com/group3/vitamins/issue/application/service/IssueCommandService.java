package com.group3.vitamins.issue.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.issue.application.command.CreateIssueCommand;
import com.group3.vitamins.issue.application.command.DeleteIssueCommand;
import com.group3.vitamins.issue.application.port.IssueAssigneePort;
import com.group3.vitamins.issue.application.port.IssueBlockPort;
import com.group3.vitamins.issue.application.port.IssueStepAccessPort;
import com.group3.vitamins.issue.application.result.IssueResult;
import com.group3.vitamins.issue.application.usecase.IssueCommandUseCase;
import com.group3.vitamins.issue.domain.IssuePriority;
import com.group3.vitamins.issue.domain.IssueStatus;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import com.group3.vitamins.issue.domain.model.Issue;
import com.group3.vitamins.issue.domain.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class IssueCommandService implements IssueCommandUseCase {

    private static final int TITLE_MAX_LENGTH = 200;

    private final IssueRepository issueRepository;
    private final IssueStepAccessPort issueStepAccessPort;
    private final IssueAssigneePort issueAssigneePort;
    private final IssueBlockPort issueBlockPort;

    @Override
    public IssueResult createIssue(CreateIssueCommand command) {
        validateTitle(command.title());
        IssueStatus status = parseStatus(command.status());
        IssuePriority priority = parsePriority(command.priority());
        List<String> assigneeIds = normalize(command.assigneeIds());
        List<Long> blockIds = normalize(command.blockIds());

        IssueStepAccessPort.StepAccessView step = issueStepAccessPort.requireEditable(
                command.stepId(), command.requesterUserId(), command.role());
        List<IssueAssigneePort.AssigneeView> assignees = issueAssigneePort.validateAssignable(
                step.projectId(), assigneeIds);
        List<IssueBlockPort.BlockView> blocks = issueBlockPort.validateLinkable(
                step.stepId(), blockIds);

        Issue saved = issueRepository.save(Issue.create(
                step.stepId(),
                command.title().trim(),
                command.content(),
                command.dueDate(),
                status,
                priority,
                command.requesterUserId(),
                LocalDateTime.now()));

        issueRepository.saveAssignees(saved.getIssueId(), assigneeIds);
        issueRepository.saveBlockLinks(saved.getIssueId(), blockIds);

        return toResult(saved, assignees, blocks);
    }

    @Override
    public void deleteIssue(DeleteIssueCommand command) {
        Issue issue = issueRepository.findActiveById(command.issueId())
                .orElseThrow(() -> new NotFoundException(IssueErrorCode.ISS_NOT_FOUND));

        issueStepAccessPort.requireEditable(
                issue.getStepId(), command.requesterUserId(), command.role());

        issueRepository.deleteAssignees(issue.getIssueId());
        issueRepository.deleteBlockLinks(issue.getIssueId());

        issue.delete(LocalDateTime.now());
        issueRepository.save(issue);
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty() || title.trim().length() > TITLE_MAX_LENGTH) {
            throw new ValidationException(IssueErrorCode.ISS_INVALID_REQUEST);
        }
    }

    private IssueStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return IssueStatus.TO_DO;
        }
        try {
            return IssueStatus.fromApiValue(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(IssueErrorCode.ISS_INVALID_REQUEST);
        }
    }

    private IssuePriority parsePriority(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(IssueErrorCode.ISS_INVALID_REQUEST);
        }
        try {
            return IssuePriority.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(IssueErrorCode.ISS_INVALID_REQUEST);
        }
    }

    private static <T> List<T> normalize(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new ValidationException(IssueErrorCode.ISS_INVALID_REQUEST);
        }
        return values.stream().distinct().toList();
    }

    private IssueResult toResult(Issue issue,
                                 List<IssueAssigneePort.AssigneeView> assignees,
                                 List<IssueBlockPort.BlockView> blocks) {
        return new IssueResult(
                issue.getIssueId(),
                issue.getStepId(),
                issue.getTitle(),
                issue.getContent(),
                issue.getStatus().toApiValue(),
                issue.getPriority().name(),
                issue.getDueDate(),
                issue.getCompletedAt(),
                assignees.stream()
                        .map(assignee -> new IssueResult.AssigneeResult(
                                assignee.userId(), assignee.name()))
                        .toList(),
                blocks.stream()
                        .map(block -> new IssueResult.BlockResult(
                                block.blockId(), block.title(), block.type()))
                        .toList());
    }
}
