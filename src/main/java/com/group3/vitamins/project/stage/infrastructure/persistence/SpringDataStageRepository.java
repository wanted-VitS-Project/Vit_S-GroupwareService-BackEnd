package com.group3.vitamins.project.stage.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataStageRepository extends JpaRepository<StageJpaEntity, Long> {

    /** 스테이지가 없으면 null 을 돌려준다 (JPQL MAX 의 동작). */
    @Query("select max(s.sortOrder) from StageJpaEntity s "
            + "where s.projectId = :projectId and s.deletedAt is null")
    Integer findMaxSortOrder(@Param("projectId") Long projectId);

    List<StageJpaEntity> findByProjectIdAndDeletedAtIsNullOrderBySortOrderAsc(Long projectId);
}