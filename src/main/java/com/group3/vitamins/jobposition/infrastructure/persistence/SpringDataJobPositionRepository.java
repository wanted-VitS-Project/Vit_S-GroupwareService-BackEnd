package com.group3.vitamins.jobposition.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SpringDataJobPositionRepository
        extends JpaRepository<JobPositionJpaEntity, Long> {

    /** 정렬 순서 오름차순, 같으면 직급명 오름차순 (`job-position.md` §1). */
    List<JobPositionJpaEntity> findAllByOrderBySortOrderAscNameAsc();

    /** 직급명으로 조회한다 (중복 검사용). */
    Optional<JobPositionJpaEntity> findByName(String name);

    /** 현재 최대 정렬 순서. 비어 있으면 null (서비스에서 0 으로 눕혀 +1 = 1). */
    @Query("SELECT MAX(j.sortOrder) FROM JobPositionJpaEntity j")
    Integer findMaxSortOrder();
}
