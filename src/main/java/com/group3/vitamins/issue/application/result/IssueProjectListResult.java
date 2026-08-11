package com.group3.vitamins.issue.application.result;

import java.util.List;

public record IssueProjectListResult(
        ProgressResult progress,
        List<StepIssuesResult> steps
) {

    /** 완료율은 totalIssueCount가 0이면 null이다 — 0/0 나눗셈을 응답에 노출하지 않는다. */
    public record ProgressResult(
            int totalIssueCount,
            int doneIssueCount,
            int inProgressIssueCount,
            Integer progressRate
    ) {
    }

    public record StepIssuesResult(
            Long stepId,
            String stepName,
            int totalIssueCount,
            int doneIssueCount,
            int inProgressIssueCount,
            Integer progressRate,
            List<IssueResult> issues
    ) {
    }
}
