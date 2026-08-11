package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalRevisionEligibilityPolicyTest {

    private static final Long APPROVAL_ID = 1L;
    private static final Long BLOCK_ID = 10L;
    private static final String ORIGINAL = "EMP001";
    private static final String EDITOR = "EMP002";

    @Mock
    private ApprovalRepository approvalRepository;
    @Mock
    private EmployeeCatalogPort employeeCatalogPort;
    @Mock
    private BlockCatalogPort blockCatalogPort;

    @InjectMocks
    private ApprovalRevisionEligibilityPolicy policy;

    @Test
    void firstActiveStepEditorClaimsWhenOriginalDrafterIsUnavailable() {
        Approval approval = approval(null);
        Approval claimed = approval(EDITOR);
        when(employeeCatalogPort.findEmployee(EDITOR)).thenReturn(Optional.of(active(EDITOR, "MEMBER")));
        when(blockCatalogPort.isBlockInCompany(BLOCK_ID, 1L)).thenReturn(true);
        when(employeeCatalogPort.findEmployee(ORIGINAL)).thenReturn(Optional.of(inactive(ORIGINAL)));
        when(blockCatalogPort.isStepEditor(BLOCK_ID, EDITOR, "MEMBER")).thenReturn(true);
        when(approvalRepository.assignActingDrafter(APPROVAL_ID, EDITOR)).thenReturn(claimed);

        Approval result = policy.claimActingDrafterOrThrow(approval, EDITOR);

        assertThat(result.getActingDrafterId()).isEqualTo(EDITOR);
    }

    @Test
    void editorCannotClaimWhileOriginalDrafterIsAvailable() {
        Approval approval = approval(null);
        when(employeeCatalogPort.findEmployee(EDITOR)).thenReturn(Optional.of(active(EDITOR, "MEMBER")));
        when(blockCatalogPort.isBlockInCompany(BLOCK_ID, 1L)).thenReturn(true);
        when(employeeCatalogPort.findEmployee(ORIGINAL)).thenReturn(Optional.of(active(ORIGINAL, "MEMBER")));

        assertThatThrownBy(() -> policy.claimActingDrafterOrThrow(approval, EDITOR))
                .isInstanceOf(ForbiddenException.class);
        verify(approvalRepository, never()).assignActingDrafter(APPROVAL_ID, EDITOR);
    }

    @Test
    void anotherEditorCannotTakeOverFromAvailableActingDrafter() {
        Approval approval = approval("EMP003");
        when(employeeCatalogPort.findEmployee(EDITOR)).thenReturn(Optional.of(active(EDITOR, "MEMBER")));
        when(blockCatalogPort.isBlockInCompany(BLOCK_ID, 1L)).thenReturn(true);
        when(employeeCatalogPort.findEmployee("EMP003")).thenReturn(Optional.of(active("EMP003", "MEMBER")));

        assertThatThrownBy(() -> policy.claimActingDrafterOrThrow(approval, EDITOR))
                .isInstanceOf(ForbiddenException.class);
        verify(approvalRepository, never()).assignActingDrafter(APPROVAL_ID, EDITOR);
    }

    @Test
    void adminCannotClaimActingDrafter() {
        Approval approval = approval(null);
        when(employeeCatalogPort.findEmployee(EDITOR)).thenReturn(Optional.of(active(EDITOR, "ADMIN")));

        assertThatThrownBy(() -> policy.claimActingDrafterOrThrow(approval, EDITOR))
                .isInstanceOf(ForbiddenException.class);
        verify(approvalRepository, never()).assignActingDrafter(APPROVAL_ID, EDITOR);
    }

    @Test
    void editorCanClaimWhenOriginalDrafterWasDeleted() {
        Approval approval = approval(null);
        Approval claimed = approval(EDITOR);
        when(employeeCatalogPort.findEmployee(EDITOR)).thenReturn(Optional.of(active(EDITOR, "MEMBER")));
        when(blockCatalogPort.isBlockInCompany(BLOCK_ID, 1L)).thenReturn(true);
        when(employeeCatalogPort.findEmployee(ORIGINAL)).thenReturn(Optional.empty());
        when(blockCatalogPort.isStepEditor(BLOCK_ID, EDITOR, "MEMBER")).thenReturn(true);
        when(approvalRepository.assignActingDrafter(APPROVAL_ID, EDITOR)).thenReturn(claimed);

        Approval result = policy.claimActingDrafterOrThrow(approval, EDITOR);

        assertThat(result.getActingDrafterId()).isEqualTo(EDITOR);
    }

    @Test
    void editorCanReplaceAdminActingDrafter() {
        Approval approval = approval("EMP003");
        Approval claimed = approval(EDITOR);
        when(employeeCatalogPort.findEmployee(EDITOR)).thenReturn(Optional.of(active(EDITOR, "MEMBER")));
        when(blockCatalogPort.isBlockInCompany(BLOCK_ID, 1L)).thenReturn(true);
        when(employeeCatalogPort.findEmployee("EMP003")).thenReturn(Optional.of(active("EMP003", "ADMIN")));
        when(employeeCatalogPort.findEmployee(ORIGINAL)).thenReturn(Optional.of(inactive(ORIGINAL)));
        when(blockCatalogPort.isStepEditor(BLOCK_ID, EDITOR, "MEMBER")).thenReturn(true);
        when(approvalRepository.assignActingDrafter(APPROVAL_ID, EDITOR)).thenReturn(claimed);

        Approval result = policy.claimActingDrafterOrThrow(approval, EDITOR);

        assertThat(result.getActingDrafterId()).isEqualTo(EDITOR);
    }

    private Approval approval(String actingDrafterId) {
        return Approval.reconstruct(APPROVAL_ID, BLOCK_ID, ORIGINAL, actingDrafterId,
                ApprovalStatus.REJECTED, 1, null, null, null, null);
    }

    private EmployeeSummary active(String userId, String role) {
        return new EmployeeSummary(userId, userId, null, null, role, 1L, "ACTIVE", null, null);
    }

    private EmployeeSummary inactive(String userId) {
        return new EmployeeSummary(userId, userId, null, null, "MEMBER", 1L, "INACTIVE", null, null);
    }
}
