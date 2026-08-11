package com.group3.vitamins.project.infrastructure.persistence;

import com.group3.vitamins.project.domain.model.CloseReasonCode;
import com.group3.vitamins.project.domain.model.Project;
import com.group3.vitamins.project.domain.model.ProjectStatus;
import com.group3.vitamins.project.domain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public Optional<Project> findByBidNoticeId(Long bidNoticeId, Long companyId) {
        return springDataRepository
                .findByBidNoticeIdAndCompanyIdAndDeletedAtIsNull(bidNoticeId, companyId)
                .map(ProjectMapper::toDomain);
    }

    @Override
    public Optional<Project> findById(Long projectId, Long companyId) {
        return springDataRepository
                .findByProjectIdAndCompanyIdAndDeletedAtIsNull(projectId, companyId)
                .map(ProjectMapper::toDomain);
    }

    @Override
    public int updateIfVersionMatches(Long projectId, Long companyId, String name,
                                      String description, String clientName,
                                      LocalDate startedOn, LocalDate endedOn,
                                      BigDecimal contractAmount, LocalDateTime updatedAt,
                                      int expectedVersion) {
        return springDataRepository.updateIfVersionMatches(
                projectId, companyId, name, description, clientName,
                startedOn, endedOn, contractAmount, updatedAt, expectedVersion);
    }

    @Override
    public int changeStatusIfVersionMatches(Long projectId, Long companyId, ProjectStatus status,
                                            CloseReasonCode closeReasonCode, String closeReasonNote,
                                            LocalDateTime closedAt, LocalDateTime updatedAt,
                                            int expectedVersion) {
        return springDataRepository.changeStatusIfVersionMatches(
                projectId, companyId, status, closeReasonCode, closeReasonNote,
                closedAt, updatedAt, expectedVersion);
    }
}