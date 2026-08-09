package com.group3.vitamins.project.block.application.service;

import com.group3.vitamins.project.block.application.usecase.BlockCascadeUseCase;
import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockCascadeService implements BlockCascadeUseCase {

    private final BlockRepository blockRepository;

    @Override
    public List<Long> findBlockIds(Long stepId) {
        return blockRepository.findByStepId(stepId).stream()
                .map(Block::getBlockId)
                .toList();
    }
}
