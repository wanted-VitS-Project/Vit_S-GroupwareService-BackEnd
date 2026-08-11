package com.group3.vitamins.project.step.infrastructure.persistence;

import com.group3.vitamins.project.step.domain.model.Step;

/**
 * ⚠️ {@link StepJpaEntity} 는 {@code @AllArgsConstructor} 라 <b>필드 선언 순서 = 아래 인자 순서</b>다.
 * 엔티티에 필드를 끼워 넣고 여기를 안 고치면 값이 밀린다 — 타입이 다르면 컴파일이 잡아 주지만
 * 타입이 같은 인접 필드끼리는 조용히 통과한다.
 */
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
                entity.getVersion(),
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
                domain.getVersion(),
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
