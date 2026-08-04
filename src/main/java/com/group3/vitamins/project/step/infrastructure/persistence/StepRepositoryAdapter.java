package com.group3.vitamins.project.step.infrastructure.persistence;

import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}