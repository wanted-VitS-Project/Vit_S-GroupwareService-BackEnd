package com.group3.vitamins.employeegroup.application.usecase;

import com.group3.vitamins.employeegroup.application.result.GroupListRow;
import com.group3.vitamins.employeegroup.application.result.GroupMembersResult;

import java.util.List;

/** 그룹 조회 인바운드 포트 (§1 목록·§5 구성원). 전체 사용자가 호출한다(선택 도구). */
public interface EmployeeGroupQueryUseCase {

    List<GroupListRow> listGroups(String keyword);

    /** 단건 — 수정 응답 재조회용. 없으면 {@code GRP_NOT_FOUND}. */
    GroupListRow getGroup(Long groupId);

    /** 구성원 목록(§5) — 그룹명 포함. 없는 그룹이면 {@code GRP_NOT_FOUND}. */
    GroupMembersResult getMembers(Long groupId);
}
