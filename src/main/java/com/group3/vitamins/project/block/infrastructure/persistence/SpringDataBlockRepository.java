package com.group3.vitamins.project.block.infrastructure.persistence;

import com.group3.vitamins.project.block.domain.model.BlockType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataBlockRepository extends JpaRepository<BlockJpaEntity, Long> {

    Optional<BlockJpaEntity> findByBlockIdAndDeletedAtIsNull(Long blockId);

    List<BlockJpaEntity> findByStepIdAndDeletedAtIsNullOrderByRowIndexAscSortOrderAsc(Long stepId);

    List<BlockJpaEntity> findByBlockIdInAndDeletedAtIsNull(Collection<Long> blockIds);

    boolean existsByStepIdAndTypeAndDeletedAtIsNull(Long stepId, BlockType type);

    Optional<BlockJpaEntity> findByTypeAndTypeIdAndDeletedAtIsNull(BlockType type, Long typeId);

    /** 블록이 없으면 null 을 돌려준다 (JPQL MAX 의 동작). */
    @Query("select max(b.rowIndex) from BlockJpaEntity b "
            + "where b.stepId = :stepId and b.deletedAt is null")
    Integer findMaxRowIndex(@Param("stepId") Long stepId);

    /** 그 행에 블록이 없으면 null 을 돌려준다. */
    @Query("select max(b.sortOrder) from BlockJpaEntity b "
            + "where b.stepId = :stepId and b.rowIndex = :rowIndex and b.deletedAt is null")
    Integer findMaxSortOrder(@Param("stepId") Long stepId, @Param("rowIndex") int rowIndex);
}