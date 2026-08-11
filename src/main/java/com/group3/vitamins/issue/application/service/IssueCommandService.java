package com.group3.vitamins.issue.application.service;

import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.issue.application.command.ChangeIssueStatusCommand;
import com.group3.vitamins.issue.application.command.CreateIssueCommand;
import com.group3.vitamins.issue.application.command.DeleteIssueCommand;
import com.group3.vitamins.issue.application.command.PatchField;
import com.group3.vitamins.issue.application.command.UpdateIssueCommand;
import com.group3.vitamins.issue.application.port.IssueAssigneePort;
import com.group3.vitamins.issue.application.port.IssueBlockPort;
import com.group3.vitamins.issue.application.port.IssueQueryPort;
import com.group3.vitamins.issue.application.port.IssueStepAccessPort;
import com.group3.vitamins.issue.application.result.IssueResult;
import com.group3.vitamins.issue.application.result.IssueStatusResult;
import com.group3.vitamins.issue.application.usecase.IssueCascadeUseCase;
import com.group3.vitamins.issue.application.usecase.IssueCommandUseCase;
import com.group3.vitamins.issue.domain.IssuePriority;
import com.group3.vitamins.issue.domain.IssueStatus;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import com.group3.vitamins.issue.domain.model.Issue;
import com.group3.vitamins.issue.domain.repository.IssueRepository;
import com.group3.vitamins.notification.domain.event.NotificationRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class IssueCommandService implements IssueCommandUseCase, IssueCascadeUseCase {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final String NOTIFICATION_TARGET_TYPE = "ISSUE";
    private static final String NOTIFICATION_TYPE_ASSIGNED = "ISSUE_ASSIGNED";
    private static final String NOTIFICATION_TITLE_ASSIGNED = "이슈 담당자 지정";

    private final IssueRepository issueRepository;
    private final IssueStepAccessPort issueStepAccessPort;
    private final IssueAssigneePort issueAssigneePort;
    private final IssueBlockPort issueBlockPort;
    private final IssueQueryPort issueQueryPort;
    private final DomainEventPublisher domainEventPublisher;

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
        publishIssueAssignedNotifications(
                saved.getIssueId(), step.projectId(), step.stepId(), saved.getTitle(), assigneeIds);

        return toResult(saved, assignees, blocks);
    }

    @Override
    public IssueResult updateIssue(UpdateIssueCommand command) {
        if (!hasUpdateField(command)) {
            throw new ValidationException(IssueErrorCode.ISS_INVALID_REQUEST);
        }
        validateVersion(command.version());

        Issue issue = findActiveIssueInCurrentCompany(command.issueId());

        IssueStepAccessPort.StepAccessView step = issueStepAccessPort.requireEditable(
                issue.getStepId(), command.requesterUserId(), command.role());

        String title = resolveTitle(issue.getTitle(), command.title());
        String content = resolve(issue.getContent(), command.content());
        LocalDateTime dueDate = resolveDueDate(issue.getDueDate(), command.dueDate());
        IssuePriority priority = resolvePriority(issue.getPriority(), command.priority());

        List<String> assigneeIds = normalizePatchList(command.assigneeIds());
        List<Long> blockIds = normalizePatchList(command.blockIds());
        List<String> previousAssigneeIds = command.assigneeIds().present()
                ? currentAssigneeIds(issue.getIssueId())
                : List.of();

        if (command.assigneeIds().present()) {
            issueAssigneePort.validateAssignable(step.projectId(), assigneeIds);
        }
        if (command.blockIds().present()) {
            issueBlockPort.validateLinkable(step.stepId(), blockIds);
        }

        if (hasGeneralField(command)) {
            issue.updateFields(title, content, dueDate, priority);
        }

        int updated = hasGeneralField(command)
                ? issueRepository.updateFieldsIfVersionMatches(issue, command.version())
                : issueRepository.touchIfVersionMatches(issue.getIssueId(), command.version());
        requireVersionMatch(updated);

        if (command.assigneeIds().present()) {
            issueRepository.deleteAssignees(issue.getIssueId());
            issueRepository.saveAssignees(issue.getIssueId(), assigneeIds);
            publishIssueAssignedNotifications(
                    issue.getIssueId(), step.projectId(), step.stepId(), title,
                    newlyAddedAssigneeIds(previousAssigneeIds, assigneeIds));
        }
        if (command.blockIds().present()) {
            issueRepository.deleteBlockLinks(issue.getIssueId());
            issueRepository.saveBlockLinks(issue.getIssueId(), blockIds);
        }

        return findLatestResult(issue.getIssueId());
    }

    @Override
    public IssueStatusResult changeIssueStatus(ChangeIssueStatusCommand command) {
        Issue issue = findActiveIssueInCurrentCompany(command.issueId());

        issueStepAccessPort.requireEditable(
                issue.getStepId(), command.requesterUserId(), command.role());

        IssueStatus nextStatus = parseRequiredStatus(command.status());
        if (!command.internal()) {
            validateVersion(command.version());
        }
        int expectedVersion = command.internal() ? issue.getVersion() : command.version();
        validateVersion(expectedVersion);

        if (issue.getStatus() == nextStatus) {
            return toStatusResult(issue);
        }

        issue.changeStatus(nextStatus, LocalDateTime.now());
        requireVersionMatch(issueRepository.changeStatusIfVersionMatches(issue, expectedVersion));

        Issue refreshed = issueRepository.findActiveById(issue.getIssueId())
                .orElse(issue);
        return toStatusResult(refreshed);
    }

    @Override
    public void deleteIssue(DeleteIssueCommand command) {
        Issue issue = findActiveIssueInCurrentCompany(command.issueId());

        issueStepAccessPort.requireEditable(
                issue.getStepId(), command.requesterUserId(), command.role());

        deleteIssue(issue);
    }

    /**
     * 스텝 삭제가 부르는 이슈 정리 (STP-013). 권한은 호출자가 프로젝트 EDITOR 로 이미 판정했다 —
     * 여기서 스텝 EDITOR 를 다시 보면 오버라이드 하나로 삭제 전체가 403 롤백된다.
     */
    @Override
    public void deleteIssues(Collection<Long> issueIds) {
        issueIds.forEach(issueId -> issueRepository.findActiveById(issueId)
                .ifPresent(this::deleteIssue));
    }

    /** 삭제 본체. 권한 판정이 끝난 뒤의 처리라 cascade 경로와 공유한다. */
    private void deleteIssue(Issue issue) {
        issueRepository.deleteAssignees(issue.getIssueId());
        issueRepository.deleteBlockLinks(issue.getIssueId());

        issue.delete(LocalDateTime.now());
        issueRepository.save(issue);
    }

    /** MyBatis 부모 경로 조회로 현재 회사 소유를 먼저 확인한 뒤 JPA 쓰기 대상을 연다. */
    private Issue findActiveIssueInCurrentCompany(Long issueId) {
        issueQueryPort.findIssue(issueId)
                .orElseThrow(() -> new NotFoundException(IssueErrorCode.ISS_NOT_FOUND));
        return issueRepository.findActiveById(issueId)
                .orElseThrow(() -> new NotFoundException(IssueErrorCode.ISS_NOT_FOUND));
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

    private IssueStatus parseRequiredStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(IssueErrorCode.ISS_STATUS_REQUIRED);
        }
        try {
            return IssueStatus.fromApiValue(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(IssueErrorCode.ISS_INVALID_STATUS);
        }
    }

    private void validateVersion(Integer version) {
        if (version == null || version < 1) {
            throw new ValidationException(IssueErrorCode.ISS_INVALID_REQUEST);
        }
    }

    private void requireVersionMatch(int updated) {
        if (updated == 0) {
            throw new ConflictException(IssueErrorCode.ISSUE_VERSION_CONFLICT);
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

    private boolean hasUpdateField(UpdateIssueCommand command) {
        return hasGeneralField(command)
                || command.assigneeIds().present()
                || command.blockIds().present();
    }

    private boolean hasGeneralField(UpdateIssueCommand command) {
        return command.title().present()
                || command.content().present()
                || command.dueDate().present()
                || command.priority().present();
    }

    private String resolveTitle(String current, PatchField<String> field) {
        if (!field.present()) {
            return current;
        }
        validateTitle(field.value());
        return field.value().trim();
    }

    private <T> T resolve(T current, PatchField<T> field) {
        return field.present() ? field.value() : current;
    }

    private LocalDateTime resolveDueDate(LocalDateTime current, PatchField<java.time.LocalDate> field) {
        if (!field.present()) {
            return current;
        }
        return field.value() == null ? null : field.value().atStartOfDay();
    }

    private IssuePriority resolvePriority(IssuePriority current, PatchField<String> field) {
        if (!field.present()) {
            return current;
        }
        return parsePriority(field.value());
    }

    private static <T> List<T> normalizePatchList(PatchField<List<T>> field) {
        if (!field.present()) {
            return List.of();
        }
        if (field.value() == null) {
            throw new ValidationException(IssueErrorCode.ISS_INVALID_REQUEST);
        }
        return normalize(field.value());
    }

    private List<String> currentAssigneeIds(Long issueId) {
        return issueQueryPort.findAssignees(List.of(issueId)).stream()
                .map(IssueQueryPort.AssigneeResult::userId)
                .distinct()
                .toList();
    }

    private List<String> newlyAddedAssigneeIds(List<String> previousAssigneeIds, List<String> nextAssigneeIds) {
        return nextAssigneeIds.stream()
                .filter(userId -> !previousAssigneeIds.contains(userId))
                .toList();
    }

    private void publishIssueAssignedNotifications(
            Long issueId, Long projectId, Long stepId, String issueTitle, List<String> recipientUserIds) {
        if (recipientUserIds.isEmpty()) {
            return;
        }
        String message = issueTitle + " 이슈 담당자로 지정되었습니다.";
        // FE 라우팅에 issueId만으로는 부족하다 — 상세 화면 URL이 projectId/stepId를 함께 요구해서
        // 결재의 revisionId와 같은 방식으로 클릭 시점 스냅샷을 targetContext에 담는다.
        Map<String, Object> targetContext = Map.of("projectId", projectId, "stepId", stepId);
        recipientUserIds.forEach(userId -> domainEventPublisher.publish(NotificationRequestedEvent.of(
                userId, NOTIFICATION_TYPE_ASSIGNED, NOTIFICATION_TITLE_ASSIGNED, message,
                NOTIFICATION_TARGET_TYPE, issueId, targetContext)));
    }

    private IssueResult findLatestResult(Long issueId) {
        IssueResult issue = issueQueryPort.findIssue(issueId)
                .orElseThrow(() -> new NotFoundException(IssueErrorCode.ISS_NOT_FOUND));
        Map<Long, List<IssueQueryPort.AssigneeResult>> assigneesByIssueId =
                issueQueryPort.findAssignees(List.of(issueId)).stream()
                        .collect(Collectors.groupingBy(IssueQueryPort.AssigneeResult::issueId));
        Map<Long, List<IssueQueryPort.RelatedBlockResult>> blocksByIssueId =
                issueQueryPort.findRelatedBlocks(List.of(issueId)).stream()
                        .collect(Collectors.groupingBy(IssueQueryPort.RelatedBlockResult::issueId));
        return toResult(issue, assigneesByIssueId, blocksByIssueId);
    }

    private IssueResult toResult(
            IssueResult issue,
            Map<Long, List<IssueQueryPort.AssigneeResult>> assigneesByIssueId,
            Map<Long, List<IssueQueryPort.RelatedBlockResult>> blocksByIssueId
    ) {
        return new IssueResult(
                issue.issueId(),
                issue.version(),
                issue.stepId(),
                issue.title(),
                issue.content(),
                issue.status(),
                issue.priority(),
                issue.dueDate(),
                issue.completedAt(),
                assigneesByIssueId.getOrDefault(issue.issueId(), List.of()).stream()
                        .map(assignee -> new IssueResult.AssigneeResult(
                                assignee.userId(), assignee.name(), assignee.resignedAt()))
                        .toList(),
                blocksByIssueId.getOrDefault(issue.issueId(), List.of()).stream()
                        .map(block -> new IssueResult.BlockResult(
                                block.blockId(), block.title(), block.type()))
                        .toList()
        );
    }

    private IssueResult toResult(Issue issue,
                                 List<IssueAssigneePort.AssigneeView> assignees,
                                 List<IssueBlockPort.BlockView> blocks) {
        return new IssueResult(
                issue.getIssueId(),
                issue.getVersion(),
                issue.getStepId(),
                issue.getTitle(),
                issue.getContent(),
                issue.getStatus().toApiValue(),
                issue.getPriority().name(),
                issue.getDueDate(),
                issue.getCompletedAt(),
                assignees.stream()
                        .map(assignee -> new IssueResult.AssigneeResult(
                                assignee.userId(), assignee.name(), assignee.resignedAt()))
                        .toList(),
                blocks.stream()
                        .map(block -> new IssueResult.BlockResult(
                                block.blockId(), block.title(), block.type()))
                        .toList());
    }

    private IssueStatusResult toStatusResult(Issue issue) {
        return new IssueStatusResult(
                issue.getIssueId(),
                issue.getVersion(),
                issue.getStatus().toApiValue(),
                issue.getCompletedAt(),
                issue.getUpdatedAt());
    }
}
