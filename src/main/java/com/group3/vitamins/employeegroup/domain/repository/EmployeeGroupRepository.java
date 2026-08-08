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

    Optional<EmployeeGroup> findById(Long groupId);

    /** 그룹명 전역 중복 검증 (생성). */
    boolean existsByName(String name);

    /** 그룹명 전역 중복 검증 (수정) — 자기 자신 제외. */
    boolean existsByNameExcludingSelf(String name, Long groupId);

    /** 그룹을 제거한다 (하드 삭제 · 구성원은 CASCADE). */
    void delete(EmployeeGroup group);
}
