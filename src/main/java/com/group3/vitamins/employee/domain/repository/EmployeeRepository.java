package com.group3.vitamins.employee.domain.repository;

import com.group3.vitamins.employee.domain.model.Employee;

import java.util.Optional;

/**
 * 사원 쓰기·단건 조회 아웃바운드 포트. 도메인은 이 인터페이스만 알고, 실제 영속은
 * {@code infrastructure/persistence} 어댑터가 처리한다.
 *
 * <p>목록·상세처럼 계정·부서·직급을 가로지르는 조회는 여기가 아니라
 * {@link com.group3.vitamins.employee.application.port.EmployeeAdminQueryPort}(MyBatis)가 맡는다.
 * 역할을 섞지 마라.
 */
public interface EmployeeRepository {

    /**
     * 사원을 저장한다. 구현은 {@code saveAndFlush} 로 즉시 반영해, PK({@code user_id}) 중복을 커밋까지
     * 미루지 않고 이 시점에 드러낸다 — 등록 유스케이스가 그 위반을 명세의 409({@code EMP_USER_ID_DUPLICATED})로
     * 변환할 수 있게 한다(사전 존재 검사와 INSERT 사이의 레이스 방어).
     */
    Employee save(Employee employee);

    /** 사번 중복 사전 검사 (등록). */
    boolean existsById(String userId);

    /** 단건 조회 (수정·퇴사 대상 확인). */
    Optional<Employee> findById(String userId);

    /**
     * 기존 사원을 수정한다 (수정·퇴사). 저장된 엔티티를 불러 필드를 갱신하므로 <b>UPDATE</b> 다
     * (등록의 {@link #save} 는 {@code Persistable} 로 INSERT 를 강제하는 것과 대비). 구현은 {@code saveAndFlush}
     * 로 즉시 반영해 FK 위반 등을 쓰기 시점에 드러낸다.
     */
    Employee update(Employee employee);
}
