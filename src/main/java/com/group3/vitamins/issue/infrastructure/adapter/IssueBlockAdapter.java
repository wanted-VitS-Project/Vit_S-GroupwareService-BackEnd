package com.group3.vitamins.issue.infrastructure.adapter;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.issue.application.port.IssueBlockPort;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueBlockAdapter implements IssueBlockPort {

    private final BlockRepository blockRepository;

    @Override
    public List<BlockView> validateLinkable(Long stepId, List<Long> blockIds) {
        if (blockIds.isEmpty()) {
            return List.of();
        }

        List<Block> blocks = blockRepository.findAllByIds(blockIds);
        if (blocks.size() != blockIds.size()) {
            throw new NotFoundException(IssueErrorCode.ISS_BLOCK_NOT_FOUND);
        }

        Map<Long, Block> byId = blocks.stream()
                .collect(Collectors.toMap(Block::getBlockId, Function.identity()));
        return blockIds.stream()
                .map(byId::get)
                .map(block -> {
                    if (!stepId.equals(block.getStepId())) {
                        throw new ValidationException(IssueErrorCode.ISS_BLOCK_STEP_MISMATCH);
                    }
                    return new BlockView(block.getBlockId(), block.getTitle(), block.getType().name());
                })
                .toList();
    }
}
