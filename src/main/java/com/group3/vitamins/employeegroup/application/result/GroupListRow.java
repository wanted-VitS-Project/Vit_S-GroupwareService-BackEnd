package com.group3.vitamins.employeegroup.application.result;

import java.time.LocalDateTime;

/**
 * 그룹 목록/단건 조회(§1·§3 응답) projection (MyBatis). memberCount 는 시스템 계정·퇴사자를 제외한 구성원 수,
 * createdByName 은 생성자 이름(스냅샷 아님·조인). 목록과 수정 응답이 같은 구조를 쓴다.
 */
public record GroupListRow(
        Long groupId,
        String name,
        String description,
        int memberCount,
        String createdByName,
        LocalDateTime createdAt
) {
}
