package com.group3.vitamins.project.step.application.query;

public record StepDetailQuery(
        Long stepId,
        String requesterUserId,
        String role
) {
}