package com.group3.vitamins.project.step.infrastructure.adapter;

import com.group3.vitamins.project.block.application.command.DeleteBlockCommand;
import com.group3.vitamins.project.block.application.command.MoveBlockCommand;
import com.group3.vitamins.project.block.application.usecase.BlockCascadeUseCase;
import com.group3.vitamins.project.block.application.usecase.BlockCommandUseCase;
import com.group3.vitamins.project.step.application.port.StepBlockCascadePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StepBlockCascadeAdapter implements StepBlockCascadePort {

    private final BlockCascadeUseCase blockCascadeUseCase;
    private final BlockCommandUseCase blockCommandUseCase;

    @Override
    public List<Long> findBlockIds(Long stepId) {
        return blockCascadeUseCase.findBlockIds(stepId);
    }

    @Override
    public void moveBlocks(Collection<Long> blockIds, Long toStepId,
                           String requesterUserId, String role) {
        blockIds.forEach(blockId -> blockCommandUseCase.moveBlock(
                new MoveBlockCommand(blockId, toStepId, requesterUserId, role)));
    }

    @Override
    public void deleteBlocks(Collection<Long> blockIds, String requesterUserId, String role) {
        blockIds.forEach(blockId -> blockCommandUseCase.deleteBlock(
                new DeleteBlockCommand(blockId, requesterUserId, role)));
    }
}
