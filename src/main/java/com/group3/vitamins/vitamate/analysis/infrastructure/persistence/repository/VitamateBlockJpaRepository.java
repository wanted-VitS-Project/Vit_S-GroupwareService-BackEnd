package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity.VitamateBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

// vitamate_block 상세 행을 저장하고 논리 삭제하는 JPA Repository
public interface VitamateBlockJpaRepository extends JpaRepository<VitamateBlockEntity, Long> {

    // 삭제되지 않은 AI 블록 상세 행만 논리 삭제한다.
    @Modifying(clearAutomatically = true)
    @Query("""
        update VitamateBlockEntity block
        set block.deletedAt = :deletedAt,
            block.updatedAt = :deletedAt
        where block.id = :vitamateBlockId
          and block.deletedAt is null
        """)
    int markDeletedIfActive(
            @Param("vitamateBlockId") Long vitamateBlockId,
            @Param("deletedAt") LocalDateTime deletedAt
    );
}
