package com.group3.vitamins.project.step.infrastructure.persistence;

import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.domain.repository.StepPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Override
    public Map<String, MemberPermission> findAllByStepId(Long stepId) {
        return springDataRepository.findByStepId(stepId).stream()
                .collect(Collectors.toMap(
                        StepPermissionJpaEntity::getUserId,
                        StepPermissionJpaEntity::getPermission));
    }

    /**
     * 기존 행이 있으면 그 PK 를 그대로 물려 UPDATE 가 되게 한다.
     * PK 를 빼먹으면 (step_id, user_id) UNIQUE 에 걸려 INSERT 가 실패한다.
     */
    @Override
    public void save(Long stepId, String userId, MemberPermission permission, LocalDateTime now) {
        Long id = springDataRepository.findByStepIdAndUserId(stepId, userId)
                .map(StepPermissionJpaEntity::getStepPermissionId)
                .orElse(null);

        springDataRepository.save(
                new StepPermissionJpaEntity(id, stepId, userId, permission, now));
    }

    @Override
    public boolean deleteByStepIdAndUserId(Long stepId, String userId) {
        return springDataRepository.deleteByStepIdAndUserId(stepId, userId) > 0;
    }

    @Override
    public void deleteByProjectIdAndUserId(Long projectId, String userId) {
        springDataRepository.deleteByProjectIdAndUserId(projectId, userId);
    }
}
