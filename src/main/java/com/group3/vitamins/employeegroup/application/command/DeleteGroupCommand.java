package com.group3.vitamins.employeegroup.application.command;

/** 그룹 삭제 (§4). 구성원이 있어도 삭제되며 매핑은 CASCADE 로 정리된다. */
public record DeleteGroupCommand(String role, Long groupId) {
}
