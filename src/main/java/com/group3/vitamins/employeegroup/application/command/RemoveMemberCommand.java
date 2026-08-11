package com.group3.vitamins.employeegroup.application.command;

/** 구성원 제거(§7) — 한 명씩. */
public record RemoveMemberCommand(String role, Long groupId, String userId) {
}
