package com.group3.vitamins.issue.application.service;

import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.issue.application.command.ChangeIssueStatusCommand;
import com.group3.vitamins.issue.application.command.CreateIssueCommand;
import com.group3.vitamins.issue.application.command.DeleteIssueCommand;
import com.group3.vitamins.issue.application.command.PatchField;
import com.group3.vitamins.issue.application.command.UpdateIssueCommand;
import com.group3.vitamins.issue.application.result.IssueStatusResult;
import com.group3.vitamins.issue.application.port.IssueAssigneePort;
import com.group3.vitamins.issue.application.port.IssueBlockPort;
import com.group3.vitamins.issue.application.port.IssueQueryPort;
import com.group3.vitamins.issue.application.port.IssueStepAccessPort;
import com.group3.vitamins.issue.application.result.IssueResult;
import com.group3.vitamins.issue.domain.IssuePriority;
import com.group3.vitamins.issue.domain.IssueStatus;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import com.group3.vitamins.issue.domain.model.Issue;
import com.group3.vitamins.issue.domain.repository.IssueRepository;
import com.group3.vitamins.notification.domain.event.NotificationRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Issue Command Service")
class IssueCommandServiceTest {

    private final IssueRepository issueRepository = mock(IssueRepository.class);
    private final IssueStepAccessPort issueStepAccessPort = mock(IssueStepAccessPort.class);
    private final IssueAssigneePort issueAssigneePort = mock(IssueAssigneePort.class);
    private final IssueBlockPort issueBlockPort = mock(IssueBlockPort.class);
    private final IssueQueryPort issueQueryPort = mock(IssueQueryPort.class);
    private final DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
    private final IssueCommandService service = new IssueCommandService(
            issueRepository,
            issueStepAccessPort,
            issueAssigneePort,
            issueBlockPort,
            issueQueryPort,
            domainEventPublisher
    );

    @Test
    @DisplayName("이슈 생성 시 담당자별 ISSUE_ASSIGNED 알림을 발행한다")
    void createIssue_publishAssignedNotifications() {
        when(issueStepAccessPort.requireEditable(10L, "EMP002", "MEMBER"))
                .thenReturn(new IssueStepAccessPort.StepAccessView(10L, 20L));
        when(issueAssigneePort.validateAssignable(20L, List.of("EMP003", "EMP005")))
                .thenReturn(List.of(
                        new IssueAssigneePort.AssigneeView("EMP003", "김용준", null),
                        new IssueAssigneePort.AssigneeView("EMP005", "김동훈", null)
                ));
        when(issueBlockPort.validateLinkable(10L, List.of()))
                .thenReturn(List.of());
        when(issueRepository.save(org.mockito.ArgumentMatchers.any(Issue.class)))
                .thenReturn(issue(101L, IssueStatus.TO_DO, null));

        service.createIssue(new CreateIssueCommand(
                10L,
                "경쟁사 제안서 벤치마킹",
                "신규 이슈",
                null,
                "TODO",
                "HIGH",
                List.of("EMP003", "EMP005"),
                List.of(),
                "EMP002",
                "MEMBER"
        ));

        verify(domainEventPublisher).publish(argThat(event ->
                event instanceof NotificationRequestedEvent notification
                        && notification.recipientUserId().equals("EMP003")
                        && notification.notificationType().equals("ISSUE_ASSIGNED")
                        && notification.targetType().equals("ISSUE")
                        && notification.targetId().equals(101L)
                        && notification.targetContext().equals(Map.of("projectId", 20L, "stepId", 10L))));
        verify(domainEventPublisher).publish(argThat(event ->
                event instanceof NotificationRequestedEvent notification
                        && notification.recipientUserId().equals("EMP005")
                        && notification.notificationType().equals("ISSUE_ASSIGNED")
                        && notification.targetType().equals("ISSUE")
                        && notification.targetId().equals(101L)
                        && notification.targetContext().equals(Map.of("projectId", 20L, "stepId", 10L))));
    }

    @Test
    @DisplayName("전달된 일반 필드와 관계 목록만 수정하고 최신 상세를 반환한다")
    void updateIssue_success() {
        Issue issue = issue(101L, IssueStatus.IN_PROGRESS, null);
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.of(issue));
        when(issueStepAccessPort.requireEditable(10L, "EMP002", "MEMBER"))
                .thenReturn(new IssueStepAccessPort.StepAccessView(10L, 20L));
        when(issueAssigneePort.validateAssignable(20L, List.of("EMP003", "EMP005")))
                .thenReturn(List.of(
                        new IssueAssigneePort.AssigneeView("EMP003", "김용준", null),
                        new IssueAssigneePort.AssigneeView("EMP005", "김동훈", null)
                ));
        when(issueBlockPort.validateLinkable(10L, List.of(15L)))
                .thenReturn(List.of(new IssueBlockPort.BlockView(15L, "제안서 작성 체크리스트", "CHECKLIST")));
        when(issueRepository.updateFieldsIfVersionMatches(issue, 1)).thenReturn(1);
        stubLatestIssue();

        IssueResult result = service.updateIssue(new UpdateIssueCommand(
                101L,
                1,
                PatchField.present(" 제안서 최종 초안 작성 "),
                PatchField.present("수정 내용"),
                PatchField.present(LocalDate.of(2026, 8, 7)),
                PatchField.present("HIGH"),
                PatchField.present(List.of("EMP003", "EMP005")),
                PatchField.present(List.of(15L)),
                "EMP002",
                "MEMBER"
        ));

        assertThat(issue.getTitle()).isEqualTo("제안서 최종 초안 작성");
        assertThat(issue.getContent()).isEqualTo("수정 내용");
        assertThat(issue.getDueDate()).isEqualTo(LocalDateTime.of(2026, 8, 7, 0, 0));
        assertThat(issue.getPriority()).isEqualTo(IssuePriority.HIGH);
        assertThat(result.issueId()).isEqualTo(101L);
        verify(issueRepository).deleteAssignees(101L);
        verify(issueRepository).saveAssignees(101L, List.of("EMP003", "EMP005"));
        verify(issueRepository).deleteBlockLinks(101L);
        verify(issueRepository).saveBlockLinks(101L, List.of(15L));
        verify(issueRepository).updateFieldsIfVersionMatches(issue, 1);
        verify(domainEventPublisher, never()).publish(argThat(event ->
                event instanceof NotificationRequestedEvent notification
                        && notification.recipientUserId().equals("EMP003")));
        verify(domainEventPublisher).publish(argThat(event ->
                event instanceof NotificationRequestedEvent notification
                        && notification.recipientUserId().equals("EMP005")
                        && notification.notificationType().equals("ISSUE_ASSIGNED")
                        && notification.targetType().equals("ISSUE")
                        && notification.targetId().equals(101L)
                        && notification.targetContext().equals(Map.of("projectId", 20L, "stepId", 10L))));
    }

    @Test
    @DisplayName("content와 dueDate는 명시적 null로 삭제할 수 있다")
    void updateIssue_nullableFields() {
        Issue issue = issue(101L, IssueStatus.IN_PROGRESS, null);
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.of(issue));
        when(issueStepAccessPort.requireEditable(10L, "EMP002", "MEMBER"))
                .thenReturn(new IssueStepAccessPort.StepAccessView(10L, 20L));
        when(issueRepository.updateFieldsIfVersionMatches(issue, 1)).thenReturn(1);
        stubLatestIssue();

        service.updateIssue(new UpdateIssueCommand(
                101L,
                1,
                PatchField.absent(),
                PatchField.present(null),
                PatchField.present(null),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.absent(),
                "EMP002",
                "MEMBER"
        ));

        assertThat(issue.getContent()).isNull();
        assertThat(issue.getDueDate()).isNull();
        verify(issueRepository, never()).deleteAssignees(101L);
        verify(issueRepository, never()).deleteBlockLinks(101L);
    }

    @Test
    @DisplayName("일반 필드의 버전이 오래됐으면 관계와 알림을 변경하지 않고 409를 던진다")
    void updateIssue_versionConflict() {
        Issue issue = issue(101L, IssueStatus.IN_PROGRESS, null);
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.of(issue));
        when(issueStepAccessPort.requireEditable(10L, "EMP002", "MEMBER"))
                .thenReturn(new IssueStepAccessPort.StepAccessView(10L, 20L));
        when(issueAssigneePort.validateAssignable(20L, List.of("EMP003"))).thenReturn(List.of());
        when(issueQueryPort.findAssignees(List.of(101L))).thenReturn(List.of());
        when(issueRepository.updateFieldsIfVersionMatches(issue, 1)).thenReturn(0);

        assertThatThrownBy(() -> service.updateIssue(new UpdateIssueCommand(
                101L,
                1,
                PatchField.present("A의 제목"),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.present(List.of("EMP003")),
                PatchField.absent(),
                "EMP002",
                "MEMBER"
        )))
                .isInstanceOfSatisfying(ConflictException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(IssueErrorCode.ISSUE_VERSION_CONFLICT));

        verify(issueRepository).updateFieldsIfVersionMatches(issue, 1);
        verify(issueRepository, never()).deleteAssignees(101L);
        verify(issueRepository, never()).saveAssignees(101L, List.of("EMP003"));
        verifyNoInteractions(domainEventPublisher);
    }

    @Test
    @DisplayName("관계 목록만 수정할 때도 버전이 오래됐으면 관계를 변경하지 않고 409를 던진다")
    void updateIssue_relationOnlyVersionConflict() {
        Issue issue = issue(101L, IssueStatus.IN_PROGRESS, null);
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.of(issue));
        when(issueStepAccessPort.requireEditable(10L, "EMP002", "MEMBER"))
                .thenReturn(new IssueStepAccessPort.StepAccessView(10L, 20L));
        when(issueBlockPort.validateLinkable(10L, List.of(15L))).thenReturn(List.of());
        when(issueRepository.touchIfVersionMatches(101L, 1)).thenReturn(0);

        assertThatThrownBy(() -> service.updateIssue(new UpdateIssueCommand(
                101L,
                1,
                PatchField.absent(),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.present(List.of(15L)),
                "EMP002",
                "MEMBER"
        )))
                .isInstanceOfSatisfying(ConflictException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(IssueErrorCode.ISSUE_VERSION_CONFLICT));

        verify(issueRepository).touchIfVersionMatches(101L, 1);
        verify(issueRepository, never()).deleteBlockLinks(101L);
        verify(issueRepository, never()).saveBlockLinks(101L, List.of(15L));
    }

    @Test
    @DisplayName("수정할 필드가 없으면 ISS_INVALID_REQUEST를 던진다")
    void updateIssue_noFields() {
        assertThatThrownBy(() -> service.updateIssue(new UpdateIssueCommand(
                101L,
                1,
                PatchField.absent(),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.absent(),
                "EMP002",
                "MEMBER"
        )))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(IssueErrorCode.ISS_INVALID_REQUEST));

        verifyNoInteractions(issueRepository);
    }

    @Test
    @DisplayName("assigneeIds null 전달은 ISS_INVALID_REQUEST를 던진다")
    void updateIssue_nullAssigneeIds() {
        Issue issue = issue(101L, IssueStatus.IN_PROGRESS, null);
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.of(issue));
        when(issueStepAccessPort.requireEditable(10L, "EMP002", "MEMBER"))
                .thenReturn(new IssueStepAccessPort.StepAccessView(10L, 20L));

        assertThatThrownBy(() -> service.updateIssue(new UpdateIssueCommand(
                101L,
                1,
                PatchField.absent(),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.present(null),
                PatchField.absent(),
                "EMP002",
                "MEMBER"
        )))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(IssueErrorCode.ISS_INVALID_REQUEST));

        verify(issueAssigneePort, never()).validateAssignable(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("이슈 상태를 DONE으로 변경하면 완료 시각을 기록하고 변경 결과를 반환한다")
    void changeIssueStatus_toDone() {
        Issue issue = issue(101L, IssueStatus.TO_DO, null);
        when(issueRepository.findActiveById(101L))
                .thenReturn(Optional.of(issue), Optional.of(issue));
        when(issueRepository.changeStatusIfVersionMatches(issue, 1)).thenReturn(1);

        IssueStatusResult result = service.changeIssueStatus(
                new ChangeIssueStatusCommand(101L, "DONE", "EMP002", "MEMBER"));

        assertThat(result.issueId()).isEqualTo(101L);
        assertThat(result.status()).isEqualTo("DONE");
        assertThat(result.completedAt()).isNotNull();
        verify(issueStepAccessPort).requireEditable(10L, "EMP002", "MEMBER");
        verify(issueRepository).changeStatusIfVersionMatches(issue, 1);
    }

    @Test
    @DisplayName("DONE 상태에서 TODO로 변경하면 완료 시각을 제거한다")
    void changeIssueStatus_doneToTodo() {
        Issue issue = issue(
                101L,
                IssueStatus.DONE,
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        when(issueRepository.findActiveById(101L))
                .thenReturn(Optional.of(issue), Optional.of(issue));
        when(issueRepository.changeStatusIfVersionMatches(issue, 1)).thenReturn(1);

        IssueStatusResult result = service.changeIssueStatus(
                new ChangeIssueStatusCommand(101L, "TODO", "EMP002", "MEMBER"));

        assertThat(result.status()).isEqualTo("TODO");
        assertThat(result.completedAt()).isNull();
        verify(issueRepository).changeStatusIfVersionMatches(issue, 1);
    }

    @Test
    @DisplayName("상태 변경의 버전이 오래됐으면 409를 던진다")
    void changeIssueStatus_versionConflict() {
        Issue issue = issue(101L, IssueStatus.TO_DO, null);
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.of(issue));
        when(issueRepository.changeStatusIfVersionMatches(issue, 1)).thenReturn(0);

        assertThatThrownBy(() -> service.changeIssueStatus(
                new ChangeIssueStatusCommand(101L, "DONE", 1, "EMP002", "MEMBER")))
                .isInstanceOfSatisfying(ConflictException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(IssueErrorCode.ISSUE_VERSION_CONFLICT));

        verify(issueRepository).changeStatusIfVersionMatches(issue, 1);
    }

    @Test
    @DisplayName("동일 상태 요청은 상태와 완료 시각을 변경하지 않고 저장하지 않는다")
    void changeIssueStatus_sameStatus() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 1, 10, 0);
        Issue issue = issue(101L, IssueStatus.DONE, completedAt);
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.of(issue));

        IssueStatusResult result = service.changeIssueStatus(
                new ChangeIssueStatusCommand(101L, "DONE", "EMP002", "MEMBER"));

        assertThat(result.status()).isEqualTo("DONE");
        assertThat(result.completedAt()).isEqualTo(completedAt);
        verify(issueRepository, never()).changeStatusIfVersionMatches(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("상태가 전달되지 않으면 ISS_STATUS_REQUIRED를 던진다")
    void changeIssueStatus_statusRequired() {
        Issue issue = issue(101L, IssueStatus.TO_DO, null);
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> service.changeIssueStatus(
                new ChangeIssueStatusCommand(101L, " ", "EMP002", "MEMBER")))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(IssueErrorCode.ISS_STATUS_REQUIRED));

        verify(issueStepAccessPort).requireEditable(10L, "EMP002", "MEMBER");
        verify(issueRepository, never()).changeStatusIfVersionMatches(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("지원하지 않는 상태이면 ISS_INVALID_STATUS를 던진다")
    void changeIssueStatus_invalidStatus() {
        Issue issue = issue(101L, IssueStatus.TO_DO, null);
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> service.changeIssueStatus(
                new ChangeIssueStatusCommand(101L, "BLOCKED", "EMP002", "MEMBER")))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(IssueErrorCode.ISS_INVALID_STATUS));

        verify(issueStepAccessPort).requireEditable(10L, "EMP002", "MEMBER");
        verify(issueRepository, never()).changeStatusIfVersionMatches(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("상태 변경 대상 이슈가 없으면 권한 확인 없이 404를 던진다")
    void changeIssueStatus_notFound() {
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeIssueStatus(
                new ChangeIssueStatusCommand(101L, "DONE", "EMP002", "MEMBER")))
                .isInstanceOfSatisfying(NotFoundException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(IssueErrorCode.ISS_NOT_FOUND));

        verifyNoInteractions(issueStepAccessPort);
        verify(issueRepository, never()).changeStatusIfVersionMatches(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("상태 변경 권한이 없으면 예외를 전파하고 저장하지 않는다")
    void changeIssueStatus_forbidden() {
        Issue issue = issue(101L, IssueStatus.TO_DO, null);
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.of(issue));
        when(issueStepAccessPort.requireEditable(10L, "EMP002", "MEMBER"))
                .thenThrow(new ForbiddenException(IssueErrorCode.ISS_EDIT_PERMISSION_REQUIRED));

        assertThatThrownBy(() -> service.changeIssueStatus(
                new ChangeIssueStatusCommand(101L, "DONE", "EMP002", "MEMBER")))
                .isInstanceOfSatisfying(ForbiddenException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                IssueErrorCode.ISS_EDIT_PERMISSION_REQUIRED));

        verify(issueRepository, never()).changeStatusIfVersionMatches(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("이슈 삭제 시 Step 편집 권한을 확인한 뒤 논리 삭제하고 관계를 제거한다")
    void deleteIssue_success() {
        Issue issue = issue(101L, IssueStatus.TO_DO, null);
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.of(issue));

        service.deleteIssue(new DeleteIssueCommand(101L, "EMP002", "MEMBER"));

        assertThat(issue.getDeletedAt()).isNotNull();
        verify(issueStepAccessPort).requireEditable(10L, "EMP002", "MEMBER");

        // Hibernate 는 @Modifying(clearAutomatically = true) 벌크 삭제 시 flush 없이 컨텍스트를 비운다 —
        // save()를 관계 삭제보다 먼저 하면 아직 flush 안 된 deletedAt 이 유실된다(BlockCommandService 와 같은 함정).
        // 그래서 관계 삭제를 먼저, Issue 본체 save()를 마지막에 호출해야 한다.
        InOrder inOrder = inOrder(issueRepository);
        inOrder.verify(issueRepository).findActiveById(101L);
        inOrder.verify(issueRepository).deleteAssignees(101L);
        inOrder.verify(issueRepository).deleteBlockLinks(101L);
        inOrder.verify(issueRepository).save(issue);
    }

    @Test
    @DisplayName("이슈가 없거나 이미 삭제됐으면 권한 확인 없이 404를 던진다")
    void deleteIssue_notFound() {
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteIssue(new DeleteIssueCommand(101L, "EMP002", "MEMBER")))
                .isInstanceOfSatisfying(NotFoundException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(IssueErrorCode.ISS_NOT_FOUND));

        verify(issueStepAccessPort, never()).requireEditable(10L, "EMP002", "MEMBER");
        verify(issueRepository, never()).deleteAssignees(101L);
        verify(issueRepository, never()).deleteBlockLinks(101L);
    }

    private Issue issue(Long issueId, IssueStatus status, LocalDateTime completedAt) {
        return Issue.restore(
                issueId,
                10L,
                "경쟁사 제안서 벤치마킹",
                "기존 내용",
                LocalDateTime.of(2026, 8, 5, 0, 0),
                status,
                IssuePriority.HIGH,
                1,
                "EMP001",
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 1, 10, 0),
                completedAt,
                null
        );
    }

    private void stubLatestIssue() {
        when(issueQueryPort.findIssue(101L)).thenReturn(Optional.of(new IssueResult(
                101L,
                2,
                10L,
                "제안서 최종 초안 작성",
                "수정 내용",
                "IN_PROGRESS",
                "HIGH",
                LocalDateTime.of(2026, 8, 7, 0, 0),
                null,
                List.of(),
                List.of()
        )));
        when(issueQueryPort.findAssignees(List.of(101L))).thenReturn(List.of(
                new IssueQueryPort.AssigneeResult(101L, "EMP003", "김용준", null)
        ));
        when(issueQueryPort.findRelatedBlocks(List.of(101L))).thenReturn(List.of(
                new IssueQueryPort.RelatedBlockResult(101L, 15L, "제안서 작성 체크리스트", "CHECKLIST")
        ));
    }
}
