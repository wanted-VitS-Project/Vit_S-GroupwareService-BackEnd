package com.group3.vitamins.project.stage.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataStagePermissionDefaultRepository
        extends JpaRepository<StagePermissionDefaultJpaEntity, Long> {

    List<StagePermissionDefaultJpaEntity> findByStageId(Long stageId);

    Optional<StagePermissionDefaultJpaEntity> findByStageIdAndUserId(Long stageId, String userId);

    void deleteByStageId(Long stageId);

    @Modifying
    @Query("DELETE FROM StagePermissionDefaultJpaEntity d "
            + "WHERE d.userId = :userId "
            + "AND d.stageId IN (SELECT s.stageId FROM StageJpaEntity s WHERE s.projectId = :projectId)")
    void deleteByProjectIdAndUserId(@Param("projectId") Long projectId,
                                    @Param("userId") String userId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM StagePermissionDefaultJpaEntity d "
            + "WHERE d.stageId IN (SELECT s.stageId FROM StageJpaEntity s WHERE s.projectId = :projectId)")
    void deleteByProjectId(@Param("projectId") Long projectId);
}
