package com.group3.vitamins.pagepermission.application.command;

/** 페이지 권한 회수(§5) 커맨드 — 페이지 코드 + 대상 사번. */
public record RevokePermissionCommand(
        String requesterRole,
        String pageCode,
        String userId
) {
}
