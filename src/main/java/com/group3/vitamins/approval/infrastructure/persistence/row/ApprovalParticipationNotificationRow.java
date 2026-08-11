package com.group3.vitamins.approval.infrastructure.persistence.row;

/** 참여 불가 알림에 필요한 결재·현재 회차 최소 조회 결과. */
public record ApprovalParticipationNotificationRow(
        Long approvalId,
        Long revisionId,
        Long blockId,
        String title,
        String drafterId,
        String actingDrafterId
) {
}
