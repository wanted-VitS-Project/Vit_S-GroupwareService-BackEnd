package com.group3.vitamins.employeegroup.application.command;

/**
 * 그룹 이름·설명 수정 (§3). PATCH 라 전달한 필드만 바꾼다 — {@code *Provided} 로 "생략" 과 "null 로 지정" 을 구분한다
 * (description 은 null 로 지울 수 있어야 하므로 값만으로는 판별 불가).
 */
public record UpdateGroupCommand(
        String role,
        Long groupId,
        boolean nameProvided,
        String name,
        boolean descriptionProvided,
        String description
) {

    public boolean hasNoFields() {
        return !nameProvided && !descriptionProvided;
    }
}
