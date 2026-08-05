package com.group3.vitamins.project.stage.infrastructure.persistence;

import com.group3.vitamins.project.stage.domain.model.Stage;

public class StageMapper {

    private StageMapper() {
    }

    /** JPA 엔티티를 도메인 객체로 복원한다. */
    public static Stage toDomain(StageJpaEntity entity) {
        return Stage.restore(
                entity.getStageId(),
                entity.getProjectId(),
                entity.getName(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getDeletedAt()
        );
    }

    /** 도메인 객체를 JPA 엔티티로 옮긴다. */
    public static StageJpaEntity toEntity(Stage domain) {
        return new StageJpaEntity(
                domain.getStageId(),
                domain.getProjectId(),
                domain.getName(),
                domain.getSortOrder(),
                domain.getCreatedAt(),
                domain.getDeletedAt()
        );
    }
}