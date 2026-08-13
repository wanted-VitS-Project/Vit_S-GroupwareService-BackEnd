package com.group3.vitamins.project.infrastructure.persistence;

import com.group3.vitamins.project.domain.model.ProjectMember;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import com.group3.vitamins.project.domain.model.MemberPermission;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProjectMemberRepositoryAdapter implements ProjectMemberRepository {

    private final SpringDataProjectMemberRepository springDataRepository;

    @Override
    public ProjectMember save(ProjectMember member) {
        return ProjectMemberMapper.toDomain(
                springDataRepository.save(ProjectMemberMapper.toEntity(member)));
    }

    @Override
    public Optional<MemberPermission> findPermission(Long projectId, String userId) {
        return springDataRepository.findByProjectIdAndUserId(projectId, userId)
                .map(ProjectMemberJpaEntity::getPermission);
    }

    @Override
    public Optional<ProjectMember> findById(Long projectMemberId) {
        return springDataRepository.findById(projectMemberId)
                .map(ProjectMemberMapper::toDomain);
    }

    @Override
    public void deleteById(Long projectMemberId) {
        springDataRepository.deleteById(projectMemberId);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        springDataRepository.deleteByProjectId(projectId);
    }
}