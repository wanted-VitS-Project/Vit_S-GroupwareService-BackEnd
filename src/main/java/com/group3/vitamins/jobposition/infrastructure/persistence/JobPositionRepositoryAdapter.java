package com.group3.vitamins.jobposition.infrastructure.persistence;

import com.group3.vitamins.jobposition.domain.model.JobPosition;
import com.group3.vitamins.jobposition.domain.repository.JobPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JobPositionRepositoryAdapter implements JobPositionRepository {

    private final SpringDataJobPositionRepository springDataRepository;

    @Override
    public List<JobPosition> findAllOrdered() {
        return springDataRepository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(JobPositionMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<JobPosition> findByName(String name) {
        return springDataRepository.findByName(name)
                .map(JobPositionMapper::toDomain);
    }

    @Override
    public Optional<JobPosition> findById(Long jobPositionId) {
        return springDataRepository.findById(jobPositionId)
                .map(JobPositionMapper::toDomain);
    }

    @Override
    public int nextSortOrder() {
        Integer max = springDataRepository.findMaxSortOrder();
        return (max == null ? 0 : max) + 1;
    }

    @Override
    public JobPosition save(JobPosition jobPosition) {
        return JobPositionMapper.toDomain(
                springDataRepository.save(JobPositionMapper.toEntity(jobPosition)));
    }

    @Override
    public void delete(JobPosition jobPosition) {
        springDataRepository.deleteById(jobPosition.getJobPositionId());
    }
}
