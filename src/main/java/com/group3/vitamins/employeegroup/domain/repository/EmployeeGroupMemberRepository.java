package com.group3.vitamins.employeegroup.domain.repository;

import java.util.Collection;
import java.util.Set;

/**
 * 그룹 구성원 매핑 쓰기 아웃바운드 포트. 구성원 목록(부서/직급 조인)·집계는
 * {@code application.port.EmployeeGroupQueryPort}(MyBatis)가 맡는다.
 */
public interface EmployeeGroupMemberRepository {

    /**
     * 요청 사번 중 <b>이미 구성원인</b> 사번만 (멱등 추가 판정). 전체 구성원을 로드하지 않고 요청분만 {@code IN} 조회한다
     * — 그룹이 커져도 추가 비용이 요청 크기에만 비례한다.
     */
    Set<String> findExistingMemberUserIds(Long groupId, Collection<String> userIds);

    /**
     * 구성원으로 추가한다(신규만 넘어온다). 빈 컬렉션이면 아무것도 하지 않는다.
     * ⚠️ 구현은 <b>즉시 flush</b> 한다 — 같은 트랜잭션에서 뒤이어 MyBatis 로 구성원 수를 읽으므로 안 그러면 방금 추가분이 안 보인다.
     */
    void addMembers(Long groupId, Collection<String> userIds);

    boolean existsMember(Long groupId, String userId);

    /** 구성원 1명 제거. 벌크 DELETE 라 즉시 DB 에 반영돼(같은 트랜잭션) 뒤따르는 MyBatis 집계가 본다. */
    void removeMember(Long groupId, String userId);
}
