package com.group3.vitamins.employee.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 사원 학력 쓰기·전체교체 삭제 (Spring Data JPA). 상세 조회(마스터명 조인)는 MyBatis 가 맡는다. */
public interface SpringDataEmployeeEducationRepository
        extends JpaRepository<EmployeeEducationJpaEntity, Long> {

    /** 벌크 DELETE — 전체 교체 수정의 앞 단계. 즉시 DB 실행돼 뒤이은 saveAll INSERT 와 순서가 보장된다. */
    @Modifying
    @Query("delete from EmployeeEducationJpaEntity e where e.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
