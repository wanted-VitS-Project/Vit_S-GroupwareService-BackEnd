package com.group3.vitamins.employeegroup.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** 그룹 구성원 매핑 쓰기·존재검증 (Spring Data JPA). 구성원 목록(부서/직급 조인)은 MyBatis 가 맡는다. */
public interface SpringDataEmployeeGroupMemberRepository
        extends JpaRepository<EmployeeGroupMemberJpaEntity, EmployeeGroupMemberId> {

    /** 요청 사번 중 이미 구성원인 사번 (멱등 판정) — 전체 로드 대신 IN 조회. */
    @Query("select m.userId from EmployeeGroupMemberJpaEntity m where m.groupId = :groupId and m.userId in :userIds")
    List<String> findExistingUserIds(@Param("groupId") Long groupId, @Param("userIds") Collection<String> userIds);

    boolean existsByGroupIdAndUserId(Long groupId, String userId);

    /** 벌크 DELETE — 즉시 DB 에 실행돼 같은 트랜잭션의 후속 MyBatis 집계가 반영값을 본다(select-then-delete 아님). */
    @Modifying
    @Query("delete from EmployeeGroupMemberJpaEntity m where m.groupId = :groupId and m.userId = :userId")
    int deleteMember(@Param("groupId") Long groupId, @Param("userId") String userId);
}
