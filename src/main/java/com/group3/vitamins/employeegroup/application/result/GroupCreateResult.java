package com.group3.vitamins.employeegroup.application.result;

/** 그룹 생성(§2) 결과 — 갓 만든 그룹은 구성원이 없으므로 memberCount 는 항상 0. */
public record GroupCreateResult(Long groupId, String name, String description, int memberCount) {
}
