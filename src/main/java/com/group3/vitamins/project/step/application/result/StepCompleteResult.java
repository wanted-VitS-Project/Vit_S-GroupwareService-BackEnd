package com.group3.vitamins.project.step.application.result;

import java.time.LocalDateTime;

/**
 * 스텝 완료 결과.
 * openIssueCount 는 완료 시점의 미완료 이슈 수이고, closedIssueCount 는 그중 실제로 닫은 수다(KEEP 이면 0).
 */
public record StepCompleteResult(
        Long stepId,
        String status,
        int openIssueCount,
        String openIssueAction,
        int closedIssueCount,
        StepPerson completedBy,
        LocalDateTime completedAt
) {
}
