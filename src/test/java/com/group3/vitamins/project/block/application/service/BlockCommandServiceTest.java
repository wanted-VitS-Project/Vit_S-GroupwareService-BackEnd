package com.group3.vitamins.project.block.application.service;

import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.block.application.command.CreateBlockCommand;
import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BlockCommandService 활동 로그 수집")
class BlockCommandServiceTest {

    private static final Long STEP_ID = 10L;
    private static final Long PROJECT_ID = 20L;
    private static final Long BLOCK_ID = 30L;
    private static final String REQUESTER_ID = "EMP001";
    private static final String ROLE = "MEMBER";

    private BlockRepository blockRepository;
    private StepAccessUseCase stepAccessUseCase;
    private DomainEventPublisher domainEventPublisher;
    private BlockCommandService blockCommandService;

    @BeforeEach
    void setUp() {
        blockRepository = mock(BlockRepository.class);
        EmployeeLookupPort employeeLookupPort = mock(EmployeeLookupPort.class);
        stepAccessUseCase = mock(StepAccessUseCase.class);
        domainEventPublisher = mock(DomainEventPublisher.class);
        BlockDetailRegistry blockDetailRegistry = new BlockDetailRegistry(List.of());

        blockCommandService = new BlockCommandService(
                blockRepository, employeeLookupPort, blockDetailRegistry, stepAccessUseCase, domainEventPublisher);

        when(stepAccessUseCase.requireEditable(STEP_ID, REQUESTER_ID, ROLE))
                .thenReturn(new StepAccessUseCase.StepAccessView(STEP_ID, PROJECT_ID, MemberPermission.EDITOR));
        when(blockRepository.findMaxRowIndex(STEP_ID)).thenReturn(Optional.empty());
        when(blockRepository.findMaxSortOrder(STEP_ID, 0)).thenReturn(Optional.empty());
        when(blockRepository.existsByStepIdAndType(STEP_ID, BlockType.TEXT)).thenReturn(false);
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> assignBlockId(invocation.getArgument(0)));
    }

    @Test
    @DisplayName("블록 생성 후 CREATE 활동 로그 이벤트를 발행한다")
    void publishesActivityLogEventAfterBlockCreated() {
        CreateBlockCommand command = new CreateBlockCommand(
                STEP_ID, "TEXT", "제안서", null, null, null, null, REQUESTER_ID, ROLE);

        blockCommandService.createBlock(command);

        ArgumentCaptor<ActivityOccurredEvent> captor = ArgumentCaptor.forClass(ActivityOccurredEvent.class);
        verify(domainEventPublisher).publish(captor.capture());

        ActivityOccurredEvent event = captor.getValue();
        assertThat(event.action()).isEqualTo(ActivityLogAction.CREATE);
        assertThat(event.blockId()).isEqualTo(BLOCK_ID);
        assertThat(event.resourceId()).isNull();
        assertThat(event.actorId()).isEqualTo(REQUESTER_ID);
        assertThat(event.changes()).hasSize(1);
        assertThat(event.changes().get(0).field()).isNull();
        assertThat(event.changes().get(0).beforeValue()).isNull();
        assertThat(event.changes().get(0).afterValue()).isNull();
    }

    private Block assignBlockId(Block block) {
        if (block.getBlockId() != null) {
            return block;
        }
        LocalDateTime now = block.getCreatedAt();
        return Block.restore(
                BLOCK_ID,
                block.getStepId(),
                block.getTitle(),
                block.getType(),
                block.getTypeId(),
                block.getOwner(),
                block.getRowIndex(),
                block.getColSpan(),
                block.getSortOrder(),
                block.getCreatedBy(),
                now,
                now,
                null
        );
    }
}
