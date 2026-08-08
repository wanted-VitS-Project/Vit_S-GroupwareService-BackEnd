package com.group3.vitamins.employeegroup.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 그룹 쓰기·단건 조회 (Spring Data JPA). 목록·구성원 집계는 {@code infrastructure/adapter}(MyBatis)가 맡는다.
 */
public interface SpringDataEmployeeGroupRepository extends JpaRepository<EmployeeGroupJpaEntity, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndGroupIdNot(String name, Long groupId);
}
