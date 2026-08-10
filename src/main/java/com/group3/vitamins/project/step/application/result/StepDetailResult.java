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
        String myPermission,

        /** 🚨 조회 응답에 반드시 실어 보낸다 — 없으면 프론트가 보낼 값이 없다 (`CONCURRENCY.md` §6-3). */
        int version
) {
}