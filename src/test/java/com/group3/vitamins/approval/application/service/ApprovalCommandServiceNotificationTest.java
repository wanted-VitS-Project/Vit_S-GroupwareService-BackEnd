package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.command.ApproveApprovalLineCommand;
import com.group3.vitamins.approval.application.command.RejectApprovalLineCommand;
import com.group3.vitamins.approval.application.policy.ApprovalLineProcessingPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalDocumentEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalLineEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalRevisionEligibilityPolicy;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.event.DomainEvent;
import com.group3.vitamins.notification.domain.event.NotificationRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 결재 → 알림 이동 대상 계약 검증 (NOTI-V1 `GEN-005` · `VIW-010`).
 *
 * <p>결재는 알림 연동의 <b>레퍼런스 구현</b>이라 다른 도메인이 이 형태를 따라간다. 그래서 발행되는
 * 페이로드({@code targetType}/{@code targetId}/{@code targetContext})를 경로별로 고정해 둔다.
 *
 * <p>특히 {@code revisionId}는 <b>알림 생성 시점 스냅샷</b>이어야 한다(VIW-010) — 클릭 시점의 최신
 * 회차로 바뀌면 알림 문구와 목적지가 어긋난다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalCommandService — 알림 이동 대상")
class ApprovalCommandServiceNotificationTest {

    private static final Long APPROVAL_ID = 55L;
    private static final Long REVISION_ID = 56L;
    private static final Long BLOCK_ID = 10L;
    private static final String DRAFTER = "EMP001";
    private static final String ACTING_DRAFTER = "EMP004";
    private static final String APPROVER_1 = "EMP002";
    private static final String APPROVER_2 = "EMP003";

    @Mock
    private ApprovalRevisionEligibilityPolicy revisionEligibilityPolicy;
    @Mock
    private ApprovalLineEligibilityPolicy lineEligibilityPolicy;
    @Mock
    private ApprovalLineProcessingPolicy lineProcessingPolicy;
    @Mock
    private ApprovalDocumentEligibilityPolicy documentEligibilityPolicy;
    @Mock
    private EmployeeCatalogPort employeeCatalogPort;
    @Mock
    private FileCatalogPort fileCatalogPort;
    @Mock
    private ApprovalRepository approvalRepository;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private ApprovalCommandService service;

    private Approval approval(ApprovalStatus status) {
        return approval(status, null);
    }

    private Approval approval(ApprovalStatus status, String actingDrafterId) {
        return Approval.reconstruct(
                APPROVAL_ID, BLOCK_ID, DRAFTER, actingDrafterId, status, 1, null, null, null, null);
    }

    private ApprovalRevision revision() {
        return ApprovalRevision.reconstruct(REVISION_ID, APPROVAL_ID, 1, "출장비 정산", "내용",
                ApprovalStatus.IN_PROGRESS, null, null, null, null, null);
    }

    private ApprovalLine line(Long lineId, String approverId, int sequenceNo, ApprovalLineStatus status) {
        return ApprovalLine.reconstruct(lineId, REVISION_ID, approverId, sequenceNo, status,
                null, null, null, null);
    }

    /** 발행된 이벤트 중 알림 이벤트만 골라낸다 — 같은 메서드가 활동 로그 이벤트도 발행한다 */
    private List<NotificationRequestedEvent> capturedNotifications() {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(domainEventPublisher, atLeastOnce()).publish(captor.capture());
        return captor.getAllValues().stream()
                .filter(NotificationRequestedEvent.class::isInstance)
                .map(NotificationRequestedEvent.class::cast)
                .toList();
    }

    private void assertPointsToApproval(NotificationRequestedEvent event) {
        assertThat(event.targetType()).isEqualTo("APPROVAL");
        assertThat(event.targetId()).isEqualTo(APPROVAL_ID);
        assertThat(event.targetContext()).containsEntry("revisionId", REVISION_ID);
    }

    @Test
    @DisplayName("승인 시 다음 결재자가 있으면 그 결재자에게 요청 알림을 보낸다")
    void approveNotifiesNextApprover() {
        ApprovalLine current = line(1L, APPROVER_1, 1, ApprovalLineStatus.ACTIVE);
        ApprovalLine next = line(2L, APPROVER_2, 2, ApprovalLineStatus.WAITING);

        when(lineProcessingPolicy.getApprovalForLineForUpdateOrThrow(1L))
                .thenReturn(approval(ApprovalStatus.IN_PROGRESS));
        when(lineProcessingPolicy.getActiveOwnedLineOrThrow(1L, APPROVER_1)).thenReturn(current);
        when(approvalRepository.findRevisionById(REVISION_ID)).thenReturn(Optional.of(revision()));
        when(approvalRepository.markLineProcessed(1L, ApprovalLineStatus.APPROVED, null))
                .thenReturn(line(1L, APPROVER_1, 1, ApprovalLineStatus.APPROVED));
        when(approvalRepository.findLineBySequenceNo(REVISION_ID, 2)).thenReturn(Optional.of(next));
        when(approvalRepository.activateLine(2L))
                .thenReturn(line(2L, APPROVER_2, 2, ApprovalLineStatus.ACTIVE));

        service.approve(new ApproveApprovalLineCommand(1L, null, APPROVER_1));

        org.mockito.InOrder lockOrder = inOrder(lineProcessingPolicy);
        lockOrder.verify(lineProcessingPolicy).getApprovalForLineForUpdateOrThrow(1L);
        lockOrder.verify(lineProcessingPolicy).getActiveOwnedLineOrThrow(1L, APPROVER_1);

        List<NotificationRequestedEvent> events = capturedNotifications();
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.recipientUserId()).isEqualTo(APPROVER_2);
            assertThat(event.notificationType()).isEqualTo("APPROVAL_REQUESTED");
            assertPointsToApproval(event);
        });
    }

    @Test
    @DisplayName("마지막 순번 승인 시 기안자에게 완료 알림을 보낸다")
    void approveNotifiesDrafterOnCompletion() {
        ApprovalLine current = line(1L, APPROVER_1, 1, ApprovalLineStatus.ACTIVE);

        when(lineProcessingPolicy.getApprovalForLineForUpdateOrThrow(1L))
                .thenReturn(approval(ApprovalStatus.IN_PROGRESS));
        when(lineProcessingPolicy.getActiveOwnedLineOrThrow(1L, APPROVER_1)).thenReturn(current);
        when(approvalRepository.findRevisionById(REVISION_ID)).thenReturn(Optional.of(revision()));
        when(approvalRepository.markLineProcessed(1L, ApprovalLineStatus.APPROVED, null))
                .thenReturn(line(1L, APPROVER_1, 1, ApprovalLineStatus.APPROVED));
        when(approvalRepository.findLineBySequenceNo(REVISION_ID, 2)).thenReturn(Optional.empty());

        service.approve(new ApproveApprovalLineCommand(1L, null, APPROVER_1));

        List<NotificationRequestedEvent> events = capturedNotifications();
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.recipientUserId()).isEqualTo(DRAFTER);
            assertThat(event.notificationType()).isEqualTo("APPROVAL_COMPLETED");
            assertPointsToApproval(event);
        });
    }

    @Test
    @DisplayName("반려 시 기안자에게 반려 알림을 보낸다")
    void rejectNotifiesDrafter() {
        ApprovalLine current = line(1L, APPROVER_1, 1, ApprovalLineStatus.ACTIVE);

        when(lineProcessingPolicy.getApprovalForLineForUpdateOrThrow(1L))
                .thenReturn(approval(ApprovalStatus.IN_PROGRESS));
        when(lineProcessingPolicy.getActiveOwnedLineOrThrow(1L, APPROVER_1)).thenReturn(current);
        when(approvalRepository.findRevisionById(REVISION_ID)).thenReturn(Optional.of(revision()));
        when(approvalRepository.markLineProcessed(1L, ApprovalLineStatus.REJECTED, "보완 필요"))
                .thenReturn(line(1L, APPROVER_1, 1, ApprovalLineStatus.REJECTED));

        service.reject(new RejectApprovalLineCommand(1L, "보완 필요", APPROVER_1));

        List<NotificationRequestedEvent> events = capturedNotifications();
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.recipientUserId()).isEqualTo(DRAFTER);
            assertThat(event.notificationType()).isEqualTo("APPROVAL_REJECTED");
            assertPointsToApproval(event);
        });
    }

    @Test
    @DisplayName("마지막 순번 승인 시 대행 기안자가 있으면 대행자에게 완료 알림을 보낸다")
    void approveNotifiesActingDrafterOnCompletion() {
        ApprovalLine current = line(1L, APPROVER_1, 1, ApprovalLineStatus.ACTIVE);

        when(lineProcessingPolicy.getApprovalForLineForUpdateOrThrow(1L))
                .thenReturn(approval(ApprovalStatus.IN_PROGRESS, ACTING_DRAFTER));
        when(lineProcessingPolicy.getActiveOwnedLineOrThrow(1L, APPROVER_1)).thenReturn(current);
        when(approvalRepository.findRevisionById(REVISION_ID)).thenReturn(Optional.of(revision()));
        when(approvalRepository.markLineProcessed(1L, ApprovalLineStatus.APPROVED, null))
                .thenReturn(line(1L, APPROVER_1, 1, ApprovalLineStatus.APPROVED));
        when(approvalRepository.findLineBySequenceNo(REVISION_ID, 2)).thenReturn(Optional.empty());

        service.approve(new ApproveApprovalLineCommand(1L, null, APPROVER_1));

        assertThat(capturedNotifications()).singleElement().satisfies(event -> {
            assertThat(event.recipientUserId()).isEqualTo(ACTING_DRAFTER);
            assertThat(event.notificationType()).isEqualTo("APPROVAL_COMPLETED");
            assertPointsToApproval(event);
        });
    }

    @Test
    @DisplayName("반려 시 대행 기안자가 있으면 대행자에게 반려 알림을 보낸다")
    void rejectNotifiesActingDrafter() {
        ApprovalLine current = line(1L, APPROVER_1, 1, ApprovalLineStatus.ACTIVE);

        when(lineProcessingPolicy.getApprovalForLineForUpdateOrThrow(1L))
                .thenReturn(approval(ApprovalStatus.IN_PROGRESS, ACTING_DRAFTER));
        when(lineProcessingPolicy.getActiveOwnedLineOrThrow(1L, APPROVER_1)).thenReturn(current);
        when(approvalRepository.findRevisionById(REVISION_ID)).thenReturn(Optional.of(revision()));
        when(approvalRepository.markLineProcessed(1L, ApprovalLineStatus.REJECTED, "보완 필요"))
                .thenReturn(line(1L, APPROVER_1, 1, ApprovalLineStatus.REJECTED));

        service.reject(new RejectApprovalLineCommand(1L, "보완 필요", APPROVER_1));

        assertThat(capturedNotifications()).singleElement().satisfies(event -> {
            assertThat(event.recipientUserId()).isEqualTo(ACTING_DRAFTER);
            assertThat(event.notificationType()).isEqualTo("APPROVAL_REJECTED");
            assertPointsToApproval(event);
        });
    }
}
