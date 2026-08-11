package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.BlockSummary;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalHandlerServiceCreationTest {

    @Mock private ApprovalRepository approvalRepository;
    @Mock private BlockCatalogPort blockCatalogPort;
    @Mock private EmployeeCatalogPort employeeCatalogPort;
    @Mock private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private ApprovalHandlerService service;

    @Test
    void adminCannotBecomeDrafterByCreatingApprovalBlock() {
        when(blockCatalogPort.findBlock(10L)).thenReturn(Optional.of(
                new BlockSummary(10L, "APPROVAL", 20L, 30L, "ADMIN001")));
        when(employeeCatalogPort.findEmployee("ADMIN001")).thenReturn(Optional.of(
                new EmployeeSummary("ADMIN001", "인사 관리자", null, null,
                        "ADMIN", 1L, "ACTIVE", null, null)));

        assertThatThrownBy(() -> service.create(10L))
                .isInstanceOf(ForbiddenException.class);
        verify(approvalRepository, never()).createDraft(10L, "ADMIN001");
    }
}
