package com.group3.vitamins.jobposition.infrastructure.persistence;

import com.group3.vitamins.jobposition.domain.model.JobPosition;

public class JobPositionMapper {

    private JobPositionMapper() {
    }

    /** JPA 엔티티를 도메인 객체로 복원한다. */
    public static JobPosition toDomain(JobPositionJpaEntity entity) {
        return JobPosition.restore(
                entity.getJobPositionId(),
                entity.getName(),
                entity.getSortOrder()
        );
    }

    /** 도메인 객체를 JPA 엔티티로 옮긴다. */
    public static JobPositionJpaEntity toEntity(JobPosition domain) {
        return new JobPositionJpaEntity(
                domain.getJobPositionId(),
                domain.getName(),
                domain.getSortOrder()
        );
    }
}
