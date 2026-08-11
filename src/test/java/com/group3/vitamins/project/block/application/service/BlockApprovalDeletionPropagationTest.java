package com.group3.vitamins.project.block.application.service;

import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.block.application.command.DeleteBlockCommand;
import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.application.port.IssueBlockUnlinkPort;
import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockApprovalDeletionPropagationTest {

    @Mock private BlockRepository blockRepository;
    @Mock private EmployeeLookupPort employeeLookupPort;
    @Mock private BlockDetailPort approvalDetailPort;
    @Mock private IssueBlockUnlinkPort issueBlockUnlinkPort;
    @Mock private StepAccessUseCase stepAccessUseCase;

    private BlockCommandService service;

    @BeforeEach
    void setUp() {
        when(approvalDetailPort.supportedType()).thenReturn(BlockType.APPROVAL);
        service = new BlockCommandService(
                blockRepository,
                employeeLookupPort,
                new BlockDetailRegistry(List.of(approvalDetailPort)),
                issueBlockUnlinkPort,
                stepAccessUseCase);
    }

    @Test
    void directBlockDeletionPropagatesSameDeletionTimeToApprovalDetail() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        Block block = Block.restore(
                10L, 20L, "구매 품의", BlockType.APPROVAL, 100L,
                null, 0, 1, 0, 0, "EMP001", createdAt, createdAt, null);
        when(blockRepository.findById(10L)).thenReturn(Optional.of(block));
        when(stepAccessUseCase.requireEditable(20L, "EMP001", "USER"))
                .thenReturn(new StepAccessUseCase.StepAccessView(20L, 30L, MemberPermission.EDITOR));
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteBlock(new DeleteBlockCommand(10L, "EMP001", "USER"));

        ArgumentCaptor<LocalDateTime> detailDeletedAt = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(approvalDetailPort).deleteDetail(
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq("EMP001"),
                org.mockito.ArgumentMatchers.eq("구매 품의"),
                detailDeletedAt.capture());
        ArgumentCaptor<Block> savedBlock = ArgumentCaptor.forClass(Block.class);
        verify(blockRepository).save(savedBlock.capture());

        assertThat(savedBlock.getValue().getDeletedAt()).isNotNull();
        assertThat(detailDeletedAt.getValue()).isEqualTo(savedBlock.getValue().getDeletedAt());
    }
}
