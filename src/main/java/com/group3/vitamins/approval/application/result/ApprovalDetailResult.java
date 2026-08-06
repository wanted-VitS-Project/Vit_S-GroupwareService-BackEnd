package com.group3.vitamins.approval.application.result;

import java.util.List;

/** 결재 상세조회(MGT-005~006) — 항상 현재 회차 기준. `documents`/`lines` 구조는 회차 상세조회(1번)와 동일하다. */
public record ApprovalDetailResult(
        Long revisionId,
        int revisionNo,
        String title,
        String content,
        String drafterId,
        String drafterName,
        String status,
        List<ApprovalDocumentView> documents,
        List<ApprovalLineDetailView> lines,
        BlockOriginView blockOrigin
) {
}
