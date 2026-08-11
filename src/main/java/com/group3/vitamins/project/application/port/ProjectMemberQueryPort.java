package com.group3.vitamins.project.application.port;

import com.group3.vitamins.project.application.result.MemberSummary;

import java.util.List;

/** 참여자를 사원 정보(이름·부서·퇴사 여부)와 함께 조회하는 아웃바운드 포트. */
public interface ProjectMemberQueryPort {

    /**
     * 이름 오름차순, 동명이인은 사번 오름차순.
     *
     * <p>⚠️ {@code employee} 는 회사별 테이블이라 {@code companyId} 로 조인을 좁힌다. 사번만으로 조인하면
     * 같은 사번이 여러 회사에 있을 때 <b>타사 사원의 이름·부서·삭제상태가 응답에 섞인다.</b>
     */
    List<MemberSummary> findMembers(Long projectId, Long companyId);
}