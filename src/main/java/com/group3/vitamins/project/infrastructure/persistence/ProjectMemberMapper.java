package com.group3.vitamins.project.infrastructure.persistence;

import com.group3.vitamins.project.domain.model.ProjectMember;

public class ProjectMemberMapper {

    private ProjectMemberMapper() {
    }

    public static ProjectMember toDomain(ProjectMemberJpaEntity entity) {
        return ProjectMember.restore(
                entity.getProjectMemberId(),
                entity.getProjectId(),
                entity.getUserId(),
                entity.getPermission(),
                entity.getCreatedAt()
        );
    }

    public static ProjectMemberJpaEntity toEntity(ProjectMember domain) {
        return new ProjectMemberJpaEntity(
                domain.getProjectMemberId(),
                domain.getProjectId(),
                domain.getUserId(),
                domain.getPermission(),
                domain.getCreatedAt()
        );
    }
}