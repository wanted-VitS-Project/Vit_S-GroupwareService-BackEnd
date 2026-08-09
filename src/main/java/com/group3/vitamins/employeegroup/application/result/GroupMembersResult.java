package com.group3.vitamins.employeegroup.application.result;

import java.time.LocalDateTime;
import java.util.List;

/** 구성원 목록(§5) 결과. departmentPath 는 조립된 파생값. */
public record GroupMembersResult(Long groupId, String name, List<Member> content) {

    public record Member(
            String userId,
            String name,
            String departmentPath,
            String jobPositionName,
            LocalDateTime addedAt
    ) {
    }
}
