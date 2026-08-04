package com.group3.vitamins.project.block.infrastructure.persistence;

import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BlockRepositoryAdapter implements BlockRepository {

    private final SpringDataBlockRepository springDataRepository;

    @Override
    public Block save(Block block) {
        return BlockMapper.toDomain(
                springDataRepository.save(BlockMapper.toEntity(block)));
    }

    @Override
    public List<Block> saveAll(List<Block> blocks) {
        return springDataRepository.saveAll(
                        blocks.stream().map(BlockMapper::toEntity).toList())
                .stream()
                .map(BlockMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Block> findById(Long blockId) {
        return springDataRepository.findByBlockIdAndDeletedAtIsNull(blockId)
                .map(BlockMapper::toDomain);
    }

    @Override
    public List<Block> findByStepId(Long stepId) {
        return springDataRepository
                .findByStepIdAndDeletedAtIsNullOrderByRowIndexAscSortOrderAsc(stepId)
                .stream()
                .map(BlockMapper::toDomain)
                .toList();
    }

    @Override
    public List<Block> findAllByIds(Collection<Long> blockIds) {
        return springDataRepository.findByBlockIdInAndDeletedAtIsNull(blockIds)
                .stream()
                .map(BlockMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Integer> findMaxRowIndex(Long stepId) {
        return Optional.ofNullable(springDataRepository.findMaxRowIndex(stepId));
    }

    @Override
    public Optional<Integer> findMaxSortOrder(Long stepId, int rowIndex) {
        return Optional.ofNullable(springDataRepository.findMaxSortOrder(stepId, rowIndex));
    }

    @Override
    public boolean existsByStepIdAndType(Long stepId, BlockType type) {
        return springDataRepository.existsByStepIdAndTypeAndDeletedAtIsNull(stepId, type);
    }

    @Override
    public Optional<Block> findByTypeAndTypeId(BlockType type, Long typeId) {
        return springDataRepository.findByTypeAndTypeIdAndDeletedAtIsNull(type, typeId)
                .map(BlockMapper::toDomain);
    }
}