package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalHandlerServiceDeletionTest {

    @Mock private ApprovalRepository approvalRepository;
    @Mock private BlockCatalogPort blockCatalogPort;
    @InjectMocks private ApprovalHandlerService service;

    @Test
    void activeApprovalIsLockedBeforeCascadeDelete() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 10, 17, 0);
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(approval(null)));

        service.deleteByBlock(100L, "EMP001", "품의", deletedAt);

        InOrder order = inOrder(approvalRepository);
        order.verify(approvalRepository).findApprovalIncludingDeletedForUpdate(100L);
        order.verify(approvalRepository).softDeleteCascade(100L, deletedAt);
    }

    @Test
    void alreadyDeletedApprovalEndsIdempotently() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 10, 17, 0);
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(approval(deletedAt.minusDays(1))));

        service.deleteByBlock(100L, "EMP001", "품의", deletedAt);

        verify(approvalRepository, never()).softDeleteCascade(100L, deletedAt);
    }

    @Test
    void missingApprovalEndsIdempotently() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 10, 17, 0);
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L)).thenReturn(Optional.empty());

        service.deleteByBlock(100L, "EMP001", "품의", deletedAt);

        verify(approvalRepository, never()).softDeleteCascade(100L, deletedAt);
    }

    private Approval approval(LocalDateTime deletedAt) {
        return Approval.reconstruct(100L, 10L, "EMP001", ApprovalStatus.IN_PROGRESS,
                1, null, null, null, deletedAt);
    }
}
