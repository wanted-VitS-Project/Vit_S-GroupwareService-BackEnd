package com.group3.vitamins.employee.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사원 쓰기·단건 조회 (Spring Data JPA). {@link EmployeeRepositoryAdapter} 가 이 인터페이스로
 * {@link com.group3.vitamins.employee.domain.repository.EmployeeRepository} 포트를 구현한다.
 *
 * <p>PK 가 {@code String}(사번)이다. 목록·상세처럼 여러 테이블을 가로지르는 조회는
 * {@code infrastructure/adapter}(MyBatis)가 맡는다.
 */
public interface SpringDataEmployeeRepository extends JpaRepository<EmployeeJpaEntity, String> {
}
