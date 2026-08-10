package com.group3.vitamins.employeegroup.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 그룹 쓰기·단건 조회 (Spring Data JPA). 목록·구성원 집계는 {@code infrastructure/adapter}(MyBatis)가 맡는다.
 */
public interface SpringDataEmployeeGroupRepository extends JpaRepository<EmployeeGroupJpaEntity, Long> {

    /** 회사 범위 단건 조회 (소유권 판정용) — 타사 그룹은 빈 결과. */
    Optional<EmployeeGroupJpaEntity> findByGroupIdAndCompanyId(Long groupId, Long companyId);

    boolean existsByNameAndCompanyId(String name, Long companyId);

    boolean existsByNameAndGroupIdNotAndCompanyId(String name, Long groupId, Long companyId);

    /** 구성원 추가 직렬화용 배타 잠금 회사 범위 단건 조회 (department 선례와 동형). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from EmployeeGroupJpaEntity g where g.groupId = :groupId and g.companyId = :companyId")
    Optional<EmployeeGroupJpaEntity> findByIdForUpdate(
            @Param("groupId") Long groupId, @Param("companyId") Long companyId);
}
