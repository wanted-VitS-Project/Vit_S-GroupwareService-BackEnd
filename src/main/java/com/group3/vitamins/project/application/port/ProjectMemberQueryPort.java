package com.group3.vitamins.project.application.port;

import com.group3.vitamins.project.application.result.MemberSummary;

import java.util.List;

/** 참여자를 사원 정보(이름·부서·퇴사 여부)와 함께 조회하는 아웃바운드 포트. */
public interface ProjectMemberQueryPort {

    /** 이름 오름차순, 동명이인은 사번 오름차순. */
    List<MemberSummary> findMembers(Long projectId);
}