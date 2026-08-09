package com.group3.vitamins.jobposition.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataJobPositionRepository
        extends JpaRepository<JobPositionJpaEntity, Long> {

    /** 회사 범위 정렬 순서 오름차순, 같으면 직급명 오름차순 (`job-position.md` §1). */
    List<JobPositionJpaEntity> findAllByCompanyIdOrderBySortOrderAscNameAsc(Long companyId);

    /** 회사 범위 직급명 조회 (중복 검사용). */
    Optional<JobPositionJpaEntity> findByNameAndCompanyId(String name, Long companyId);

    /** 회사 범위 단건 조회 (소유권 판정용) — 타사 직급은 빈 결과. */
    Optional<JobPositionJpaEntity> findByJobPositionIdAndCompanyId(Long jobPositionId, Long companyId);

    /** 현재 회사 최대 정렬 순서. 비어 있으면 null (서비스에서 0 으로 눕혀 +1 = 1). */
    @Query("SELECT MAX(j.sortOrder) FROM JobPositionJpaEntity j WHERE j.companyId = :companyId")
    Integer findMaxSortOrder(@Param("companyId") Long companyId);
}
