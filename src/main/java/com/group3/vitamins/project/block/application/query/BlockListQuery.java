package com.group3.vitamins.project.block.application.query;

public record BlockListQuery(
        Long stepId,
        String requesterUserId,
        String role
) {
}