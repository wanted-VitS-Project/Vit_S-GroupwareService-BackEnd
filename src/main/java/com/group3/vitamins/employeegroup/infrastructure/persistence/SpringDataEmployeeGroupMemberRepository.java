package com.group3.vitamins.employeegroup.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 그룹 구성원 매핑 쓰기·존재검증 (Spring Data JPA). 구성원 목록(부서/직급 조인)은 MyBatis 가 맡는다. */
public interface SpringDataEmployeeGroupMemberRepository
        extends JpaRepository<EmployeeGroupMemberJpaEntity, EmployeeGroupMemberId> {

    List<EmployeeGroupMemberJpaEntity> findByGroupId(Long groupId);

    boolean existsByGroupIdAndUserId(Long groupId, String userId);

    void deleteByGroupIdAndUserId(Long groupId, String userId);
}
