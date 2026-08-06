package com.group3.vitamins.approval.application.result;

import java.time.LocalDateTime;

/** 결재 이력조회(MGT-007)의 회차 1건. */
public record ApprovalRevisionHistoryItem(
        Long revisionId,
        int revisionNo,
        String status,
        LocalDateTime submittedAt,
        LocalDateTime finishedAt,
        boolean isCurrent
) {
}
