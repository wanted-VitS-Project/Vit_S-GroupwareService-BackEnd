package com.group3.vitamins.project.application.command;

public record CloseProjectCommand(
        Long projectId,
        String closeReasonCode,
        String closeReasonNote,
        String requesterUserId,
        String role
) {
}