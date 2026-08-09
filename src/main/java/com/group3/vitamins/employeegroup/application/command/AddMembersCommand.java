package com.group3.vitamins.employeegroup.application.command;

import java.util.List;

/** 구성원 추가(§6). {@code userIds} 1개 이상. 중복·이미 소속은 서비스가 멱등 처리한다. */
public record AddMembersCommand(String role, Long groupId, List<String> userIds) {
}
