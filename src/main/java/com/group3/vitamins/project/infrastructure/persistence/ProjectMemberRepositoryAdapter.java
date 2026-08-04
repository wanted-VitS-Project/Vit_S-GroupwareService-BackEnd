package com.group3.vitamins.project.infrastructure.persistence;

import com.group3.vitamins.project.domain.model.ProjectMember;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectMemberRepositoryAdapter implements ProjectMemberRepository {

    private final SpringDataProjectMemberRepository springDataRepository;

    @Override
    public ProjectMember save(ProjectMember member) {
        return ProjectMemberMapper.toDomain(
                springDataRepository.save(ProjectMemberMapper.toEntity(member)));
    }
}