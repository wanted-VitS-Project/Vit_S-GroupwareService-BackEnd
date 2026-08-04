package com.group3.vitamins.project.step.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataStepRepository extends JpaRepository<StepJpaEntity, Long> {

    /** 스텝이 없으면 null 을 돌려준다 (JPQL MAX 의 동작). */
    @Query("select max(s.sortOrder) from StepJpaEntity s "
            + "where s.projectId = :projectId and s.deletedAt is null")
    Integer findMaxSortOrder(@Param("projectId") Long projectId);
}