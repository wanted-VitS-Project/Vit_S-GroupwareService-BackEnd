package com.group3.vitamins.project.stage.application.command;

public record CreateStageCommand(
        Long projectId,
        String name,
        Integer sortOrder,
        String requesterUserId,
        String role
) {
}