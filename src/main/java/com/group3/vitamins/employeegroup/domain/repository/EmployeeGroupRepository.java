package com.group3.vitamins.employeegroup.domain.repository;

import com.group3.vitamins.employeegroup.domain.model.EmployeeGroup;

import java.util.Optional;

/**
 * 그룹 쓰기·단건 조회 아웃바운드 포트. 목록·구성원 집계처럼 {@code employee} 를 가로지르는 조회는
 * {@code application.port.EmployeeGroupQueryPort}(MyBatis)가 맡는다 — 역할을 섞지 마라.
 */
public interface EmployeeGroupRepository {

    /** 저장한다. 구현은 {@code saveAndFlush} 로 유니크 위반을 이 시점에 드러낸다(→ 서비스가 409 변환). */
    EmployeeGroup save(EmployeeGroup group);

    /** 회사 범위 단건 조회. 타사 그룹은 조회되지 않아 404 로 귀결된다. */
    Optional<EmployeeGroup> findById(Long groupId, Long companyId);

    /**
     * <b>비관적 쓰기 잠금</b> 회사 범위 단건 조회 — 구성원 추가(§6) 시 그룹 행을 잠가 동시 요청을 직렬화한다.
     * 두 요청이 같은 그룹에 같은 사번을 동시에 넣어 복합 PK(group_id, user_id)가 충돌하는 레이스를 원천 차단한다
     * (department {@code findByIdForUpdate} 선례). 잠금을 쥔 동안 다른 추가 요청은 대기한다.
     */
    Optional<EmployeeGroup> findByIdForUpdate(Long groupId, Long companyId);

    /** 그룹명 회사 범위 중복 검증 (생성). */
    boolean existsByName(String name, Long companyId);

    /** 그룹명 회사 범위 중복 검증 (수정) — 자기 자신 제외. */
    boolean existsByNameExcludingSelf(String name, Long groupId, Long companyId);

    /** 그룹을 제거한다 (하드 삭제 · 구성원은 CASCADE). */
    void delete(EmployeeGroup group);
}
