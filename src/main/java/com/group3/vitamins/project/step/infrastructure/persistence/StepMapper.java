package com.group3.vitamins.project.step.infrastructure.persistence;

import com.group3.vitamins.project.step.domain.model.Step;

public class StepMapper {

    private StepMapper() {
    }

    /** JPA 엔티티를 도메인 객체로 복원한다. */
    public static Step toDomain(StepJpaEntity entity) {
        return Step.restore(
                entity.getStepId(),
                entity.getProjectId(),
                entity.getStageId(),
                entity.getName(),
                entity.getSortOrder(),
                entity.getStartedOn(),
                entity.getEndedOn(),
                entity.getOwnerUserId(),
                entity.getStatus(),
                entity.getCompletedAt(),
                entity.getCompletedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }

    /** 도메인 객체를 JPA 엔티티로 옮긴다. */
    public static StepJpaEntity toEntity(Step domain) {
        return new StepJpaEntity(
                domain.getStepId(),
                domain.getProjectId(),
                domain.getStageId(),
                domain.getName(),
                domain.getSortOrder(),
                domain.getStartedOn(),
                domain.getEndedOn(),
                domain.getOwnerUserId(),
                domain.getStatus(),
                domain.getCompletedAt(),
                domain.getCompletedBy(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getDeletedAt()
        );
    }
}