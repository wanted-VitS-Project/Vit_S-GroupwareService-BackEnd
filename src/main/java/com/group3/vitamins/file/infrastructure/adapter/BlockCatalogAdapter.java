package com.group3.vitamins.file.infrastructure.adapter;

import com.group3.vitamins.file.application.port.BlockCatalogPort;
import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link BlockCatalogPort} 구현 — project.block 의 {@code BlockRepository} 를 재사용한다.
 * {@code findById} 가 soft delete 된 블록을 이미 걸러주므로(=`block.deleted_at IS NULL`),
 * 여기서는 블록 타입만 추가로 본다.
 */
@Component
@RequiredArgsConstructor
public class BlockCatalogAdapter implements BlockCatalogPort {

    private final BlockRepository blockRepository;

    @Override
    public Optional<Long> resolveFileBlockStepId(Long blockId) {
        return blockRepository.findById(blockId)
                .filter(block -> block.getType() == BlockType.FILE)
                .map(Block::getStepId);
    }

    @Override
    public Optional<Long> resolveAttachableBlockStepId(Long blockId) {
        return blockRepository.findById(blockId)
                .filter(block -> block.getType() == BlockType.FILE
                        || block.getType() == BlockType.APPROVAL)
                .map(Block::getStepId);
    }
}
