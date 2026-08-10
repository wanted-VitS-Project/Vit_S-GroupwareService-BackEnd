package com.group3.vitamins.project.step.application.command;

/** 스텝 권한 부여·변경. permission 은 문자열이다 — 허용값 판정을 서비스가 한다. */
public record SetStepPermissionCommand(
        Long stepId,
        String userId,
        String permission,
        String requesterUserId,
        String role
) {
}
