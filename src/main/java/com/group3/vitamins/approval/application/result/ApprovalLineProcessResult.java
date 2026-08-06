package com.group3.vitamins.approval.application.result;

import java.time.LocalDateTime;

/** 결재선 처리(승인·반려) 결과. */
public record ApprovalLineProcessResult(
        Long lineId,
        String status,
        LocalDateTime processedAt,
        Long nextActiveLineId,
        boolean approvalCompleted
) {
}
