package com.group3.vitamins.project.stage.infrastructure.persistence;

import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.stage.domain.repository.StagePermissionDefaultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StagePermissionDefaultRepositoryAdapter implements StagePermissionDefaultRepository {

    private final SpringDataStagePermissionDefaultRepository springDataRepository;

    @Override
    public Map<String, MemberPermission> findAllByStageId(Long stageId) {
        return springDataRepository.findByStageId(stageId).stream()
                .collect(Collectors.toMap(
                        StagePermissionDefaultJpaEntity::getUserId,
                        StagePermissionDefaultJpaEntity::getPermission));
    }

    /**
     * 기존 행이 있으면 그 PK 를 그대로 물려 UPDATE 가 되게 한다.
     * PK 를 빼먹으면 (stage_id, user_id) UNIQUE 에 걸려 두 번째 저장이 실패한다.
     */
    @Override
    public void save(Long stageId, String userId, MemberPermission permission, LocalDateTime now) {
        Long id = springDataRepository.findByStageIdAndUserId(stageId, userId)
                .map(StagePermissionDefaultJpaEntity::getStagePermissionDefaultId)
                .orElse(null);

        springDataRepository.save(
                new StagePermissionDefaultJpaEntity(id, stageId, userId, permission, now));
    }

    @Override
    public void deleteByStageId(Long stageId) {
        springDataRepository.deleteByStageId(stageId);
    }

    @Override
    public void deleteByProjectIdAndUserId(Long projectId, String userId) {
        springDataRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        springDataRepository.deleteByProjectId(projectId);
    }
}
