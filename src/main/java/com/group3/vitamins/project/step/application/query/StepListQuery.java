package com.group3.vitamins.project.step.application.query;

import com.group3.vitamins.project.step.domain.model.StepStatus;

public record StepListQuery(
        Long projectId,
        Long stageId,
        StepStatus status,
        String requesterUserId,
        String role
) {
}