package com.group3.vitamins.project.step.application.result;

import java.time.LocalDate;

public record StepSummary(
        Long stepId,
        Long stageId,
        String name,
        String status,
        int sortOrder,
        LocalDate startedOn,
        LocalDate endedOn,
        StepPerson owner,
        int totalIssueCount,
        int doneIssueCount,
        int inProgressIssueCount,
        Integer progressRate,
        String myPermission
) {
}