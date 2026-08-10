package com.group3.vitamins.project.stage.application.command;

/**
 * 하위 스텝 권한 일괄 적용 (STG-004).
 * 기본값 저장은 항상 하고, 기존 스텝 전개는 {@code applyToExistingSteps} 가 true 일 때만 한다.
 */
public record ApplyStagePermissionCommand(
        Long stageId,
        String userId,
        String permission,
        boolean applyToExistingSteps,
        String requesterUserId,
        String role
) {
}
