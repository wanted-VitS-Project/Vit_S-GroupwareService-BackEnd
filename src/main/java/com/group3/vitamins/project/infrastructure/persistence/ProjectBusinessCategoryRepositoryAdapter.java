package com.group3.vitamins.project.infrastructure.persistence;

import com.group3.vitamins.project.domain.repository.ProjectBusinessCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectBusinessCategoryRepositoryAdapter implements ProjectBusinessCategoryRepository {

    private final SpringDataProjectBusinessCategoryRepository springDataRepository;

    @Override
    public void linkAll(Long companyId, Long projectId, List<Long> businessCategoryIds) {
        List<ProjectBusinessCategoryJpaEntity> links = businessCategoryIds.stream()
                .map(categoryId -> new ProjectBusinessCategoryJpaEntity(null, companyId, projectId, categoryId))
                .toList();
        springDataRepository.saveAll(links);
    }

    @Override
    public List<Long> findCategoryIds(Long projectId) {
        return springDataRepository.findByProjectId(projectId).stream()
                .map(ProjectBusinessCategoryJpaEntity::getBusinessCategoryId)
                .toList();
    }

    @Override
    public boolean unlink(Long projectId, Long businessCategoryId) {
        return springDataRepository
                .deleteByProjectIdAndBusinessCategoryId(projectId, businessCategoryId) > 0;
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        springDataRepository.deleteByProjectId(projectId);
    }

}