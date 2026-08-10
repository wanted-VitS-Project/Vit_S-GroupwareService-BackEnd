package com.group3.vitamins.department.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 부서 쓰기·단건 조회 (Spring Data JPA). {@link DepartmentRepositoryAdapter} 가 이 인터페이스로
 * {@link com.group3.vitamins.department.domain.repository.DepartmentRepository} 포트를 구현한다.
 * 트리·인원 집계처럼 {@code employee} 를 가로지르는 조회는 {@code infrastructure/adapter}(MyBatis)가 맡는다.
 */
public interface SpringDataDepartmentRepository extends JpaRepository<DepartmentJpaEntity, Long> {

    /** 회사 범위 단건 조회 (소유권 판정용) — 타사 부서는 빈 결과 */
    Optional<DepartmentJpaEntity> findByDepartmentIdAndCompanyId(Long departmentId, Long companyId);

    /** 같은 회사·상위 부서 안 동명 검증 (생성, 하위 부서) */
    boolean existsByNameAndParentIdAndCompanyId(String name, Long parentId, Long companyId);

    /** 회사 안 최상위 형제 동명 검증 (생성, 부모 없음) — 파생 쿼리가 {@code parent_id IS NULL} 로 번역한다 */
    boolean existsByNameAndParentIdIsNullAndCompanyId(String name, Long companyId);

    /** 같은 회사·상위 부서 안 동명 검증 (수정, 하위 부서) — 자기 자신 제외 */
    boolean existsByNameAndParentIdAndDepartmentIdNotAndCompanyId(
            String name, Long parentId, Long departmentId, Long companyId);

    /** 회사 안 최상위 형제 동명 검증 (수정, 부모 없음) — 자기 자신 제외 */
    boolean existsByNameAndParentIdIsNullAndDepartmentIdNotAndCompanyId(
            String name, Long departmentId, Long companyId);

    /** 회사 범위 직속 하위 부서 수 — 삭제 차단 판정 */
    long countByParentIdAndCompanyId(Long parentId, Long companyId);

    /**
     * 삭제·생성용 <b>비관적 쓰기 잠금</b> 회사 범위 단건 조회. 부서 행을 잠가, 판정 이후 실제 쓰기 전에 다른 트랜잭션이
     * 이 부서로 사원 배정·하위 부서 생성을 하지 못하게 직렬화한다 (InnoDB 는 FK 참조 INSERT 시 부모 행에
     * 공유 잠금을 걸므로 이 배타 잠금과 충돌해 대기한다). account 의 {@code findByUserIdForUpdate} 와 같은 패턴이다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DepartmentJpaEntity d where d.departmentId = :departmentId and d.companyId = :companyId")
    Optional<DepartmentJpaEntity> findByIdForUpdate(
            @Param("departmentId") Long departmentId, @Param("companyId") Long companyId);
}
