package com.group3.vitamins.project.step.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataStepPermissionRepository
        extends JpaRepository<StepPermissionJpaEntity, Long> {

    List<StepPermissionJpaEntity> findByStepIdInAndUserId(Collection<Long> stepIds, String userId);

    Optional<StepPermissionJpaEntity> findByStepIdAndUserId(Long stepId, String userId);

    @Modifying
    @Query("DELETE FROM StepPermissionJpaEntity sp "
            + "WHERE sp.userId = :userId "
            + "AND sp.stepId IN (SELECT s.stepId FROM StepJpaEntity s WHERE s.projectId = :projectId)")
    void deleteByProjectIdAndUserId(@Param("projectId") Long projectId,
                                    @Param("userId") String userId);
}
