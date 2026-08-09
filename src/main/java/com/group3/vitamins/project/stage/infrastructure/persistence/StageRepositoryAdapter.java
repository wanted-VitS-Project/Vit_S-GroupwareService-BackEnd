package com.group3.vitamins.project.stage.infrastructure.persistence;

import com.group3.vitamins.project.stage.domain.model.Stage;
import com.group3.vitamins.project.stage.domain.repository.StageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StageRepositoryAdapter implements StageRepository {

    private final SpringDataStageRepository springDataRepository;

    @Override
    public Stage save(Stage stage) {
        return StageMapper.toDomain(
                springDataRepository.save(StageMapper.toEntity(stage)));
    }

    @Override
    public Optional<Stage> findById(Long stageId) {
        return springDataRepository.findByStageIdAndDeletedAtIsNull(stageId)
                .map(StageMapper::toDomain);
    }

    @Override
    public List<Stage> findAllByIdsInProject(Collection<Long> stageIds, Long projectId) {
        return springDataRepository
                .findByStageIdInAndProjectIdAndDeletedAtIsNull(stageIds, projectId)
                .stream()
                .map(StageMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Integer> findMaxSortOrder(Long projectId) {
        return Optional.ofNullable(springDataRepository.findMaxSortOrder(projectId));
    }

    @Override
    public List<Stage> findAllByProjectId(Long projectId) {
        return springDataRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAsc(projectId)
                .stream()
                .map(StageMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsInProject(Long stageId, Long projectId) {
        return springDataRepository.existsByStageIdAndProjectIdAndDeletedAtIsNull(stageId, projectId);
    }
}