package com.group3.vitamins.project.step.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StepDetailResult(
        Long stepId,
        Long projectId,
        Long stageId,
        String name,
        String status,
        LocalDate startedOn,
        LocalDate endedOn,
        StepPerson owner,
        int totalIssueCount,
        int doneIssueCount,
        int inProgressIssueCount,
        Integer progressRate,
        StepPerson completedBy,
        LocalDateTime completedAt,
        String myPermission
) {
}