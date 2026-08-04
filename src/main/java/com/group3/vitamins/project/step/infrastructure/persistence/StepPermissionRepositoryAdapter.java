package com.group3.vitamins.project.step.infrastructure.persistence;

import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.domain.repository.StepPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StepPermissionRepositoryAdapter implements StepPermissionRepository {

    private final SpringDataStepPermissionRepository springDataRepository;

    @Override
    public Map<Long, MemberPermission> findOverrides(Collection<Long> stepIds, String userId) {
        if (stepIds.isEmpty()) {
            return Map.of();
        }
        return springDataRepository.findByStepIdInAndUserId(stepIds, userId).stream()
                .collect(Collectors.toMap(
                        StepPermissionJpaEntity::getStepId,
                        StepPermissionJpaEntity::getPermission));
    }

    @Override
    public Optional<MemberPermission> findOverride(Long stepId, String userId) {
        return springDataRepository.findByStepIdAndUserId(stepId, userId)
                .map(StepPermissionJpaEntity::getPermission);
    }
}
