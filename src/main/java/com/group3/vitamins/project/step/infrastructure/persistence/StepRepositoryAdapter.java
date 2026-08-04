package com.group3.vitamins.project.step.infrastructure.persistence;

import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.model.StepStatus;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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