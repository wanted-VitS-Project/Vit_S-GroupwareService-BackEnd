package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.notification.domain.event.NotificationRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalHandlerServiceDeletionTest {

    private static final LocalDateTime DELETED_AT = LocalDateTime.of(2026, 8, 10, 17, 0);

    @Mock private ApprovalRepository approvalRepository;
    @Mock private BlockCatalogPort blockCatalogPort;
    @Mock private DomainEventPublisher domainEventPublisher;
    @InjectMocks private ApprovalHandlerService service;

    @Test
    void activeApprovalIsLockedBeforeCascadeDelete() {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(approval(ApprovalStatus.IN_PROGRESS, null)));

        service.deleteByBlock(100L, "EMP001", "품의", DELETED_AT);

        InOrder order = inOrder(approvalRepository);
        order.verify(approvalRepository).findApprovalIncludingDeletedForUpdate(100L);
        order.verify(approvalRepository).softDeleteCascade(100L, DELETED_AT);
    }

    @Test
    void alreadyDeletedApprovalEndsIdempotently() {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(approval(ApprovalStatus.IN_PROGRESS, DELETED_AT.minusDays(1))));

        service.deleteByBlock(100L, "EMP001", "품의", DELETED_AT);

        verify(approvalRepository, never()).softDeleteCascade(100L, DELETED_AT);
        // DEL-013 — 재시도가 알림을 중복 발행하지 않는다
        verify(domainEventPublisher, never()).publish(any());
    }

    @Test
    void missingApprovalEndsIdempotently() {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L)).thenReturn(Optional.empty());

        service.deleteByBlock(100L, "EMP001", "품의", DELETED_AT);

        verify(approvalRepository, never()).softDeleteCascade(100L, DELETED_AT);
        verify(domainEventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("DEL-011 — 진행 중 결재는 기안자와 현재 결재자에게만 취소 알림이 간다")
    void notifiesDrafterAndActiveApproverOnly() {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(approval(ApprovalStatus.IN_PROGRESS, null)));
        when(approvalRepository.findLinesByApprovalId(100L)).thenReturn(List.of(
                line(1L, "EMP002", 1, ApprovalLineStatus.APPROVED),
                line(2L, "EMP003", 2, ApprovalLineStatus.ACTIVE),
                line(3L, "EMP004", 3, ApprovalLineStatus.WAITING)));
        when(approvalRepository.findLatestRevisionReadOnly(100L)).thenReturn(Optional.of(revision("장비 구매 품의")));

        service.deleteByBlock(100L, "EMP009", "블록 제목", DELETED_AT);

        ArgumentCaptor<NotificationRequestedEvent> captor =
                ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(domainEventPublisher, org.mockito.Mockito.times(2)).publish(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(NotificationRequestedEvent::recipientUserId)
                .containsExactly("EMP001", "EMP003");
        NotificationRequestedEvent first = captor.getAllValues().get(0);
        assertThat(first.notificationType()).isEqualTo("APPROVAL_CANCELED");
        assertThat(first.message()).contains("장비 구매 품의");
        // 결재가 삭제돼 상세가 404 이므로 이동 대상을 붙이지 않는다(DEL-008)
        assertThat(first.targetType()).isNull();
        assertThat(first.targetId()).isNull();
    }

    @Test
    @DisplayName("수신자 조회는 삭제 실행보다 먼저다 — 뒤면 활성 필터에 걸려 0명이 된다")
    void resolvesRecipientsBeforeCascade() {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(approval(ApprovalStatus.IN_PROGRESS, null)));
        when(approvalRepository.findLinesByApprovalId(100L))
                .thenReturn(List.of(line(2L, "EMP003", 1, ApprovalLineStatus.ACTIVE)));

        service.deleteByBlock(100L, "EMP009", "블록 제목", DELETED_AT);

        InOrder order = inOrder(approvalRepository);
        order.verify(approvalRepository).findLinesByApprovalId(100L);
        order.verify(approvalRepository).softDeleteCascade(100L, DELETED_AT);
    }

    @Test
    @DisplayName("DRAFT 결재는 아직 요청이 가지 않았으므로 알리지 않는다")
    void doesNotNotifyForDraftApproval() {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(approval(ApprovalStatus.DRAFT, null)));

        service.deleteByBlock(100L, "EMP009", "블록 제목", DELETED_AT);

        verify(approvalRepository).softDeleteCascade(100L, DELETED_AT);
        verify(domainEventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("이미 종결된 결재는 결과를 통지받았으므로 알리지 않는다")
    void doesNotNotifyForFinishedApproval() {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(approval(ApprovalStatus.COMPLETED, null)));

        service.deleteByBlock(100L, "EMP009", "블록 제목", DELETED_AT);

        verify(domainEventPublisher, never()).publish(any());
    }

    private Approval approval(ApprovalStatus status, LocalDateTime deletedAt) {
        return Approval.reconstruct(100L, 10L, "EMP001", status, 1, null, null, null, deletedAt);
    }

    private ApprovalLine line(Long lineId, String approverId, int sequenceNo, ApprovalLineStatus status) {
        return ApprovalLine.reconstruct(lineId, 200L, approverId, sequenceNo, status, null, null, null, null);
    }

    private ApprovalRevision revision(String title) {
        return ApprovalRevision.reconstruct(200L, 100L, 1, title, "내용",
                ApprovalStatus.IN_PROGRESS, null, null, null, null, null);
    }
}
