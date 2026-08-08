package com.group3.vitamins.employeegroup.domain.repository;

import java.util.Collection;
import java.util.Set;

/**
 * 그룹 구성원 매핑 쓰기 아웃바운드 포트. 구성원 목록(부서/직급 조인)·집계는
 * {@code application.port.EmployeeGroupQueryPort}(MyBatis)가 맡는다.
 */
public interface EmployeeGroupMemberRepository {

    /** 그룹의 현재 구성원 사번 집합 (멱등 추가 판정용). */
    Set<String> findMemberUserIds(Long groupId);

    /** 구성원으로 추가한다(신규만 넘어온다). 빈 컬렉션이면 아무것도 하지 않는다. */
    void addMembers(Long groupId, Collection<String> userIds);

    boolean existsMember(Long groupId, String userId);

    void removeMember(Long groupId, String userId);
}
