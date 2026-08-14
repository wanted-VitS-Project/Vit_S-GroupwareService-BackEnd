package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.BlockClonePort;
import com.group3.vitamins.project.block.application.usecase.BlockCloneUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class BlockCloneAdapter implements BlockClonePort {

    private final BlockCloneUseCase blockCloneUseCase;

    @Override
    public int countBlocks(Long projectId) {
        return blockCloneUseCase.countByProjectId(projectId);
    }

    @Override
    public ClonedBlocks cloneBlocks(Map<Long, Long> stepIdMap, String requesterUserId) {
        BlockCloneUseCase.BlockCloneCount count =
                blockCloneUseCase.cloneToSteps(stepIdMap, requesterUserId);

        return new ClonedBlocks(count.copied(), count.skipped());
    }
}
