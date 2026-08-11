package com.group3.vitamins.project.stage.infrastructure.persistence;

import com.group3.vitamins.project.stage.domain.model.Stage;

/**
 * ⚠️ {@link StageJpaEntity} 는 {@code @AllArgsConstructor} 라 <b>필드 선언 순서 = 아래 인자 순서</b>다.
 * 엔티티에 필드를 끼워 넣고 여기를 안 고치면 타입이 같은 인접 필드끼리 값이 밀린다
 * ({@code sortOrder} ↔ {@code version} 은 둘 다 {@code int} 라 <b>컴파일이 통과한다</b>).
 */
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
                entity.getVersion(),
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
                domain.getVersion(),
                domain.getCreatedAt(),
                domain.getDeletedAt()
        );
    }
}
