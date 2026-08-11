package com.group3.vitamins.project.step.application.command;

/** 스텝 권한 회수. 오버라이드 행을 지워 프로젝트 권한 상속으로 되돌린다 (STP-011). */
public record RevokeStepPermissionCommand(
        Long stepId,
        String userId,
        String requesterUserId,
        String role
) {
}
