package com.group3.vitamins.employeegroup.application.result;

/** 구성원 추가(§6) 결과. 이미 소속이던 사번은 조용히 건너뛰고 집계에만 반영한다(멱등). */
public record AddMembersResult(
        Long groupId,
        int requestedCount,
        int addedCount,
        int alreadyMemberCount,
        int memberCount
) {
}
