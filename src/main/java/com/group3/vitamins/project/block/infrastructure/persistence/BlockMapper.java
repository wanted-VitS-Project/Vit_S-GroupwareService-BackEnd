package com.group3.vitamins.project.block.infrastructure.persistence;

import com.group3.vitamins.project.block.domain.model.Block;

/**
 * ⚠️ {@link BlockJpaEntity} 는 {@code @AllArgsConstructor} 라 <b>필드 선언 순서 = 아래 인자 순서</b>다.
 * 엔티티에 필드를 끼워 넣고 여기를 안 고치면 값이 밀린다 — {@code sortOrder} ↔ {@code version} 은
 * 둘 다 {@code int} 라 <b>컴파일이 통과한다</b>.
 */
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
                entity.getVersion(),
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
                domain.getVersion(),
                domain.getCreatedBy(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getDeletedAt()
        );
    }
}
