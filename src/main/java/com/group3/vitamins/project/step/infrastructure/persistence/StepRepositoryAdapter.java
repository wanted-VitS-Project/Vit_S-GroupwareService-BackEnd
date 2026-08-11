package com.group3.vitamins.project.step.infrastructure.persistence;

import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.model.StepStatus;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StepRepositoryAdapter implements StepRepository {

    private final SpringDataStepRepository springDataRepository;

    @Override
    public Step save(Step step) {
        return StepMapper.toDomain(
                springDataRepository.save(StepMapper.toEntity(step)));
    }

    @Override
    public Optional<Integer> findMaxSortOrder(Long projectId) {
        return Optional.ofNullable(springDataRepository.findMaxSortOrder(projectId));
    }

    @Override
    public Optional<Step> findById(Long stepId) {
        return springDataRepository.findByStepIdAndDeletedAtIsNull(stepId)
                .map(StepMapper::toDomain);
    }

    @Override
    public List<Step> findAllByStageId(Long stageId) {
        return springDataRepository.findByStageIdAndDeletedAtIsNull(stageId).stream()
                .map(StepMapper::toDomain)
                .toList();
    }

    @Override
    public List<Step> search(Long projectId, Long stageId, StepStatus status) {
        List<StepJpaEntity> rows = status == null
                ? springDataRepository.findByProjectIdAndDeletedAtIsNullOrderBySortOrderAsc(projectId)
                : springDataRepository.findByProjectIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc(
                        projectId, status);

        return rows.stream()
                .filter(row -> matchesStage(row.getStageId(), stageId))
                .map(StepMapper::toDomain)
                .toList();
    }

    @Override
    public List<Step> findAllByIdsInProject(Collection<Long> stepIds, Long projectId) {
        return springDataRepository
                .findByStepIdInAndProjectIdAndDeletedAtIsNull(stepIds, projectId)
                .stream()
                .map(StepMapper::toDomain)
                .toList();
    }

    @Override
    public int updateIfVersionMatches(Long stepId, String name, LocalDate startedOn,
                                      LocalDate endedOn, String ownerUserId,
                                      LocalDateTime updatedAt, int expectedVersion) {
        return springDataRepository.updateIfVersionMatches(
                stepId, name, startedOn, endedOn, ownerUserId, updatedAt, expectedVersion);
    }

    @Override
    public int changeStatusIfVersionMatches(Long stepId, StepStatus status,
                                            LocalDateTime completedAt, String completedBy,
                                            LocalDateTime updatedAt, int expectedVersion) {
        return springDataRepository.changeStatusIfVersionMatches(
                stepId, status, completedAt, completedBy, updatedAt, expectedVersion);
    }

    @Override
    public int moveIfVersionMatches(Long stepId, Long stageId, int sortOrder,
                                    LocalDateTime updatedAt, int expectedVersion) {
        return springDataRepository.moveIfVersionMatches(
                stepId, stageId, sortOrder, updatedAt, expectedVersion);
    }

    /** stageId 가 null 이면 전체, 0 이면 미소속만, 그 외는 해당 스테이지만 남긴다. */
    private boolean matchesStage(Long rowStageId, Long stageId) {
        if (stageId == null) {
            return true;
        }
        if (stageId == 0L) {
            return rowStageId == null;
        }
        return stageId.equals(rowStageId);
    }
}