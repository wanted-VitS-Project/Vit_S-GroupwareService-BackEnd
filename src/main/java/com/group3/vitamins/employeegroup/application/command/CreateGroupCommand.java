package com.group3.vitamins.employeegroup.application.command;

/** 그룹 생성 (§2). {@code createdBy} 는 생성자 사번(세션 주체). */
public record CreateGroupCommand(String role, String createdBy, String name, String description) {
}
