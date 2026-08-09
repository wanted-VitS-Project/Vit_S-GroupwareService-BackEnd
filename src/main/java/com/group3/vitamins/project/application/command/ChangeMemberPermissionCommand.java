package com.group3.vitamins.project.application.command;

public record ChangeMemberPermissionCommand(
        Long projectId,
        Long memberId,
        String permission,
        String requesterUserId,
        String role
) {}