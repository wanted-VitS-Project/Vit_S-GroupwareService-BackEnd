package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.command.UpdateApprovalLinesCommand;
import com.group3.vitamins.approval.application.policy.ApprovalDocumentEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalLineEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalLineProcessingPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalRevisionEligibilityPolicy;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.event.DomainEvent;
import com.group3.vitamins.notification.domain.event.NotificationRequestedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalUnavailableApproverReplacementTest {

    @Mock private ApprovalRevisionEligibilityPolicy revisionEligibilityPolicy;
    @Mock private ApprovalLineEligibilityPolicy lineEligibilityPolicy;
    @Mock private ApprovalLineProcessingPolicy lineProcessingPolicy;
    @Mock private ApprovalDocumentEligibilityPolicy documentEligibilityPolicy;
    @Mock private EmployeeCatalogPort employeeCatalogPort;
    @Mock private FileCatalogPort fileCatalogPort;
    @Mock private ApprovalRepository approvalRepository;
    @Mock private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private ApprovalCommandService service;

    @Test
    void activeUnavailableApproverIsReplacedWithSameOrderAndStatusAndNotified() {
        Approval approval = approval();
        ApprovalRevision revision = revision();
        ApprovalLine previous = line(1L, "EMP_OLD", ApprovalLineStatus.ACTIVE);
        ApprovalLine replacement = line(2L, "EMP_NEW", ApprovalLineStatus.ACTIVE);
        UpdateApprovalLinesCommand command = command("EMP_NEW");

        when(revisionEligibilityPolicy.getApprovalForUpdateOrThrow(1L)).thenReturn(approval);
        when(revisionEligibilityPolicy.getRevisionForUpdateOrThrow(1L, 2L)).thenReturn(revision);
        when(revisionEligibilityPolicy.claimActingDrafterOrThrow(approval, "EMP_DRAFTER")).thenReturn(approval);
        when(approvalRepository.findLinesByRevisionId(2L))
                .thenReturn(List.of(previous), List.of(replacement));
        when(lineEligibilityPolicy.isParticipationUnavailable("EMP_OLD")).thenReturn(true);
        when(lineEligibilityPolicy.assertApproversEligible(10L, List.of("EMP_NEW")))
                .thenReturn(List.of(activeEmployee("EMP_NEW")));
        when(approvalRepository.replaceUnavailableLine(previous, "EMP_NEW")).thenReturn(replacement);
        when(employeeCatalogPort.findEmployee("EMP_NEW")).thenReturn(Optional.of(activeEmployee("EMP_NEW")));

        var result = service.updateLines(command);

        assertThat(result).singleElement().satisfies(line -> {
            assertThat(line.lineId()).isEqualTo(2L);
            assertThat(line.approverId()).isEqualTo("EMP_NEW");
            assertThat(line.order()).isEqualTo(1);
        });
        verify(approvalRepository).replaceUnavailableLine(previous, "EMP_NEW");

        ArgumentCaptor<DomainEvent> events = ArgumentCaptor.forClass(DomainEvent.class);
        verify(domainEventPublisher, times(2)).publish(events.capture());
        NotificationRequestedEvent notification = events.getAllValues().stream()
                .filter(NotificationRequestedEvent.class::isInstance)
                .map(NotificationRequestedEvent.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(notification.recipientUserId()).isEqualTo("EMP_NEW");
        assertThat(notification.notificationType()).isEqualTo("APPROVAL_REQUESTED");
    }

    @Test
    void availableApproverCannotBeChangedDuringInProgressRevision() {
        Approval approval = approval();
        ApprovalLine previous = line(1L, "EMP_OLD", ApprovalLineStatus.ACTIVE);
        when(revisionEligibilityPolicy.getApprovalForUpdateOrThrow(1L)).thenReturn(approval);
        when(revisionEligibilityPolicy.getRevisionForUpdateOrThrow(1L, 2L)).thenReturn(revision());
        when(revisionEligibilityPolicy.claimActingDrafterOrThrow(approval, "EMP_DRAFTER")).thenReturn(approval);
        when(approvalRepository.findLinesByRevisionId(2L)).thenReturn(List.of(previous));
        when(lineEligibilityPolicy.isParticipationUnavailable("EMP_OLD")).thenReturn(false);

        assertThatThrownBy(() -> service.updateLines(command("EMP_NEW")))
                .isInstanceOf(ConflictException.class);
        verify(approvalRepository, never()).replaceUnavailableLine(previous, "EMP_NEW");
        verify(lineEligibilityPolicy, never()).assertApproversEligible(org.mockito.ArgumentMatchers.anyLong(), anyList());
    }

    @Test
    void activeUnavailableApproverCanBeExcludedAndNextWaitingLineMovesForward() {
        Approval approval = approval();
        ApprovalRevision revision = revision();
        ApprovalLine firstApproved = line(1L, "EMP_1", 1, ApprovalLineStatus.APPROVED);
        ApprovalLine unavailableActive = line(2L, "EMP_OLD", 2, ApprovalLineStatus.ACTIVE);
        ApprovalLine thirdWaiting = line(3L, "EMP_3", 3, ApprovalLineStatus.WAITING);
        ApprovalLine secondWaiting = line(3L, "EMP_3", 2, ApprovalLineStatus.WAITING);
        ApprovalLine secondActive = line(3L, "EMP_3", 2, ApprovalLineStatus.ACTIVE);

        UpdateApprovalLinesCommand command = new UpdateApprovalLinesCommand(1L, 2L, "EMP_DRAFTER",
                List.of(
                        new UpdateApprovalLinesCommand.LineInput("EMP_1", 1),
                        new UpdateApprovalLinesCommand.LineInput("EMP_3", 2)));

        when(revisionEligibilityPolicy.getApprovalForUpdateOrThrow(1L)).thenReturn(approval);
        when(revisionEligibilityPolicy.getRevisionForUpdateOrThrow(1L, 2L)).thenReturn(revision);
        when(revisionEligibilityPolicy.claimActingDrafterOrThrow(approval, "EMP_DRAFTER")).thenReturn(approval);
        when(approvalRepository.findLinesByRevisionId(2L))
                .thenReturn(List.of(firstApproved, unavailableActive, thirdWaiting));
        when(lineEligibilityPolicy.isParticipationUnavailable("EMP_OLD")).thenReturn(true);
        when(approvalRepository.excludeUnavailableLines(2L, List.of(2L)))
                .thenReturn(List.of(firstApproved, secondWaiting));
        when(approvalRepository.activateLine(3L)).thenReturn(secondActive);
        when(employeeCatalogPort.findEmployee("EMP_1")).thenReturn(Optional.of(activeEmployee("EMP_1")));
        when(employeeCatalogPort.findEmployee("EMP_3")).thenReturn(Optional.of(activeEmployee("EMP_3")));

        var result = service.updateLines(command);

        assertThat(result).extracting(line -> line.approverId() + ":" + line.order())
                .containsExactly("EMP_1:1", "EMP_3:2");
        verify(approvalRepository).excludeUnavailableLines(2L, List.of(2L));
        verify(approvalRepository).activateLine(3L);
        verify(approvalRepository, never()).finalizeApproval(1L, 2L, ApprovalStatus.COMPLETED);

        ArgumentCaptor<DomainEvent> events = ArgumentCaptor.forClass(DomainEvent.class);
        verify(domainEventPublisher, times(2)).publish(events.capture());
        NotificationRequestedEvent notification = events.getAllValues().stream()
                .filter(NotificationRequestedEvent.class::isInstance)
                .map(NotificationRequestedEvent.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(notification.recipientUserId()).isEqualTo("EMP_3");
        assertThat(notification.notificationType()).isEqualTo("APPROVAL_REQUESTED");
    }

    @Test
    void excludingLastActiveApproverCompletesWhenEarlierLinesAreApproved() {
        Approval approval = approval();
        ApprovalLine firstApproved = line(1L, "EMP_1", 1, ApprovalLineStatus.APPROVED);
        ApprovalLine unavailableActive = line(2L, "EMP_OLD", 2, ApprovalLineStatus.ACTIVE);
        UpdateApprovalLinesCommand command = new UpdateApprovalLinesCommand(1L, 2L, "EMP_DRAFTER",
                List.of(new UpdateApprovalLinesCommand.LineInput("EMP_1", 1)));

        when(revisionEligibilityPolicy.getApprovalForUpdateOrThrow(1L)).thenReturn(approval);
        when(revisionEligibilityPolicy.getRevisionForUpdateOrThrow(1L, 2L)).thenReturn(revision());
        when(revisionEligibilityPolicy.claimActingDrafterOrThrow(approval, "EMP_DRAFTER")).thenReturn(approval);
        when(approvalRepository.findLinesByRevisionId(2L))
                .thenReturn(List.of(firstApproved, unavailableActive));
        when(lineEligibilityPolicy.isParticipationUnavailable("EMP_OLD")).thenReturn(true);
        when(approvalRepository.excludeUnavailableLines(2L, List.of(2L)))
                .thenReturn(List.of(firstApproved));
        when(employeeCatalogPort.findEmployee("EMP_1")).thenReturn(Optional.of(activeEmployee("EMP_1")));

        var result = service.updateLines(command);

        assertThat(result).singleElement().satisfies(line -> {
            assertThat(line.approverId()).isEqualTo("EMP_1");
            assertThat(line.order()).isEqualTo(1);
        });
        verify(approvalRepository).finalizeApproval(1L, 2L, ApprovalStatus.COMPLETED);
        verify(approvalRepository, never()).activateLine(org.mockito.ArgumentMatchers.anyLong());

        ArgumentCaptor<DomainEvent> events = ArgumentCaptor.forClass(DomainEvent.class);
        verify(domainEventPublisher, times(2)).publish(events.capture());
        NotificationRequestedEvent notification = events.getAllValues().stream()
                .filter(NotificationRequestedEvent.class::isInstance)
                .map(NotificationRequestedEvent.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(notification.recipientUserId()).isEqualTo("EMP_DRAFTER");
        assertThat(notification.notificationType()).isEqualTo("APPROVAL_COMPLETED");
    }

    @Test
    void availableActiveApproverCannotBeExcluded() {
        Approval approval = approval();
        ApprovalLine firstApproved = line(1L, "EMP_1", 1, ApprovalLineStatus.APPROVED);
        ApprovalLine availableActive = line(2L, "EMP_2", 2, ApprovalLineStatus.ACTIVE);
        ApprovalLine thirdWaiting = line(3L, "EMP_3", 3, ApprovalLineStatus.WAITING);
        UpdateApprovalLinesCommand command = new UpdateApprovalLinesCommand(1L, 2L, "EMP_DRAFTER",
                List.of(
                        new UpdateApprovalLinesCommand.LineInput("EMP_1", 1),
                        new UpdateApprovalLinesCommand.LineInput("EMP_3", 2)));

        when(revisionEligibilityPolicy.getApprovalForUpdateOrThrow(1L)).thenReturn(approval);
        when(revisionEligibilityPolicy.getRevisionForUpdateOrThrow(1L, 2L)).thenReturn(revision());
        when(revisionEligibilityPolicy.claimActingDrafterOrThrow(approval, "EMP_DRAFTER")).thenReturn(approval);
        when(approvalRepository.findLinesByRevisionId(2L))
                .thenReturn(List.of(firstApproved, availableActive, thirdWaiting));
        when(lineEligibilityPolicy.isParticipationUnavailable("EMP_2")).thenReturn(false);

        assertThatThrownBy(() -> service.updateLines(command))
                .isInstanceOf(ConflictException.class);
        verify(approvalRepository, never()).excludeUnavailableLines(
                org.mockito.ArgumentMatchers.anyLong(), anyList());
    }

    private Approval approval() {
        return Approval.reconstruct(1L, 10L, "EMP_DRAFTER", null,
                ApprovalStatus.IN_PROGRESS, 1, null, null, null, null);
    }

    private ApprovalRevision revision() {
        return ApprovalRevision.reconstruct(2L, 1L, 1, "품의", "내용",
                ApprovalStatus.IN_PROGRESS, null, null, null, null, null);
    }

    private ApprovalLine line(Long id, String approverId, ApprovalLineStatus status) {
        return line(id, approverId, 1, status);
    }

    private ApprovalLine line(Long id, String approverId, int sequenceNo, ApprovalLineStatus status) {
        return ApprovalLine.reconstruct(id, 2L, approverId, sequenceNo, status,
                null, null, null, null);
    }

    private UpdateApprovalLinesCommand command(String approverId) {
        return new UpdateApprovalLinesCommand(1L, 2L, "EMP_DRAFTER",
                List.of(new UpdateApprovalLinesCommand.LineInput(approverId, 1)));
    }

    private EmployeeSummary activeEmployee(String userId) {
        return new EmployeeSummary(userId, "신규 결재자", null, null,
                "MEMBER", 1L, "ACTIVE", null, null);
    }
}
