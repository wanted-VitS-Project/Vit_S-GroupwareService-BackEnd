package com.group3.vitamins.project.step.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataStepPermissionRepository
        extends JpaRepository<StepPermissionJpaEntity, Long> {

    List<StepPermissionJpaEntity> findByStepIdInAndUserId(Collection<Long> stepIds, String userId);

    Optional<StepPermissionJpaEntity> findByStepIdAndUserId(Long stepId, String userId);
}
