package com.group3.vitamins.project.infrastructure.persistence;

import com.group3.vitamins.project.domain.model.Project;
import com.group3.vitamins.project.domain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryAdapter implements ProjectRepository {

    private final SpringDataProjectRepository springDataRepository;

    @Override
    public Project save(Project project) {
        return ProjectMapper.toDomain(
                springDataRepository.save(ProjectMapper.toEntity(project)));
    }

    @Override
    public Optional<Project> findByBidNoticeId(Long bidNoticeId) {
        return springDataRepository.findByBidNoticeIdAndDeletedAtIsNull(bidNoticeId)
                .map(ProjectMapper::toDomain);
    }

    @Override
    public Optional<Project> findById(Long projectId) {
        return springDataRepository.findByProjectIdAndDeletedAtIsNull(projectId)
                .map(ProjectMapper::toDomain);
    }


}