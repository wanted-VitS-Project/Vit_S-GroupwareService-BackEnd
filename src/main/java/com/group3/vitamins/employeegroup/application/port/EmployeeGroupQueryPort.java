package com.group3.vitamins.employeegroup.application.port;

import com.group3.vitamins.employeegroup.application.result.EmployeeRefRow;
import com.group3.vitamins.employeegroup.application.result.GroupListRow;
import com.group3.vitamins.employeegroup.application.result.MemberRow;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 그룹 목록·단건의 화면용 조회 아웃바운드 포트 (MyBatis). 구성원 수·생성자명처럼 {@code employee} 를 가로지르는
 * 조회를 담당한다. 쓰기·존재검증은 {@code EmployeeGroupRepository}(JPA)가 맡는다.
 */
public interface EmployeeGroupQueryPort {

    /** 그룹 목록 — 이름 오름차순. {@code keyword} 가 있으면 그룹명 부분검색. */
    List<GroupListRow> findGroups(String keyword);

    /** 단건 — 수정(§3) 응답이 목록과 같은 구조라 재조회에 쓴다. 없으면 empty. */
    Optional<GroupListRow> findGroup(Long groupId);

    /** 구성원 목록(§5) — 이름 오름차순. 시스템 계정·퇴사자 제외. */
    List<MemberRow> findMembers(Long groupId);

    /** 처리 후 구성원 수(§6·§7 응답) — 시스템 계정·퇴사자 제외(목록과 같은 기준). */
    int countMembers(Long groupId);

    /** §6 검증용 — 요청 사번 중 실재하는 사원의 (사번·시스템여부). 없는 사번은 결과에 빠진다. */
    List<EmployeeRefRow> findEmployeeRefs(Collection<String> userIds);
}
