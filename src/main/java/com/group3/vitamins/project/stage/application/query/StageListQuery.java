package com.group3.vitamins.project.stage.application.query;

public record StageListQuery(
        Long projectId,
        String requesterUserId,
        String role
) {
}