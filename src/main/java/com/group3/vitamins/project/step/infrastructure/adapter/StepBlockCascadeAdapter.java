package com.group3.vitamins.project.step.infrastructure.adapter;

import com.group3.vitamins.project.block.application.usecase.BlockCascadeUseCase;
import com.group3.vitamins.project.step.application.port.StepBlockCascadePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StepBlockCascadeAdapter implements StepBlockCascadePort {

    private final BlockCascadeUseCase blockCascadeUseCase;

    @Override
    public List<Long> findBlockIds(Long stepId) {
        return blockCascadeUseCase.findBlockIds(stepId);
    }

    @Override
    public void moveBlocks(Collection<Long> blockIds, Long toStepId) {
        blockCascadeUseCase.moveBlocks(blockIds, toStepId);
    }

    @Override
    public void deleteBlocks(Collection<Long> blockIds, String requesterUserId) {
        blockCascadeUseCase.deleteBlocks(blockIds, requesterUserId);
    }
}
