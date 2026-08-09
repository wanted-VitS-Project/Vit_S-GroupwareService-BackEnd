package com.group3.vitamins.employeegroup.domain.repository;

import java.util.Collection;
import java.util.Set;

/**
 * 그룹 구성원 매핑 쓰기 아웃바운드 포트. 구성원 목록(부서/직급 조인)·집계는
 * {@code application.port.EmployeeGroupQueryPort}(MyBatis)가 맡는다.
 */
public interface EmployeeGroupMemberRepository {

    /**
     * 요청 사번 중 <b>이미 구성원인</b> 사번만 (멱등 추가 판정). 전체 구성원을 로드하지 않고 요청분만 조회한다
     * — 그룹이 커져도 추가 비용이 요청 크기에만 비례한다.
     */
    Set<String> findExistingMemberUserIds(Long groupId, Collection<String> userIds);

    /**
     * 구성원으로 추가한다(신규만 넘어온다). 빈 컬렉션이면 아무것도 하지 않는다.
     * 추가한 결과는 <b>같은 작업 안의 뒤이은 조회에 즉시 보인다</b>(가시성 보장은 어댑터 책임).
     */
    void addMembers(Long groupId, Collection<String> userIds);

    /**
     * 구성원 1명 제거 — <b>제거된 수</b>를 돌려준다(0이면 구성원이 아니었음). 존재확인과 제거를 <b>원자적으로</b> 합쳐,
     * 동시 제거가 둘 다 사전확인을 통과한 뒤 한쪽이 아무것도 못 지우고도 성공으로 응답하는 레이스를 막는다.
     * 제거 결과는 뒤이은 조회에 즉시 보인다.
     */
    int removeMember(Long groupId, String userId);
}
