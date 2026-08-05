package com.group3.vitamins.project.block.infrastructure.persistence;

import com.group3.vitamins.project.block.domain.model.Block;

public class BlockMapper {

    private BlockMapper() {
    }

    /** JPA 엔티티를 도메인 객체로 복원한다. */
    public static Block toDomain(BlockJpaEntity entity) {
        return Block.restore(
                entity.getBlockId(),
                entity.getStepId(),
                entity.getTitle(),
                entity.getType(),
                entity.getTypeId(),
                entity.getOwner(),
                entity.getRowIndex(),
                entity.getColSpan(),
                entity.getSortOrder(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }

    /** 도메인 객체를 JPA 엔티티로 옮긴다. */
    public static BlockJpaEntity toEntity(Block domain) {
        return new BlockJpaEntity(
                domain.getBlockId(),
                domain.getStepId(),
                domain.getTitle(),
                domain.getType(),
                domain.getTypeId(),
                domain.getOwner(),
                domain.getRowIndex(),
                domain.getColSpan(),
                domain.getSortOrder(),
                domain.getCreatedBy(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getDeletedAt()
        );
    }
}