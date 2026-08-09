package com.group3.vitamins.project.application.command;

public record AddMemberCommand(
        Long projectId,
        String userId,
        String permission,
        String requesterUserId,
        String role
) {}