package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.domain.model.Project;
import com.group3.vitamins.project.infrastructure.persistence.ProjectJpaEntity;

public class ProjectMapper {

    private ProjectMapper() {
    }

    /** JPA 엔티티를 도메인 객체로 복원한다. */
    public static Project toDomain(ProjectJpaEntity entity) {
        return Project.restore(
                entity.getProjectId(),
                entity.getBidNoticeId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getClientName(),
                entity.getContractAmount(),
                entity.getStartedOn(),
                entity.getEndedOn(),
                entity.getCloseReasonCode(),
                entity.getCloseReasonNote(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }

    /** 도메인 객체를 JPA 엔티티로 옮긴다. */
    public static ProjectJpaEntity toEntity(Project domain) {
        return new ProjectJpaEntity(
                domain.getProjectId(),
                domain.getBidNoticeId(),
                domain.getName(),
                domain.getDescription(),
                domain.getStatus(),
                domain.getClientName(),
                domain.getContractAmount(),
                domain.getStartedOn(),
                domain.getEndedOn(),
                domain.getCloseReasonCode(),
                domain.getCloseReasonNote(),
                domain.getCreatedBy(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getDeletedAt()
        );
    }
}