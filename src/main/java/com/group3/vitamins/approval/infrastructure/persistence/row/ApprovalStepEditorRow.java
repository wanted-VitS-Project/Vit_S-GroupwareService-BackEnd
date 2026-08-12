package com.group3.vitamins.approval.infrastructure.persistence.row;

/** 참여 불가 기안자 알림을 받을 블록별 유효 스텝 EDITOR 조회 결과. */
public record ApprovalStepEditorRow(
        Long blockId,
        String userId
) {
}
