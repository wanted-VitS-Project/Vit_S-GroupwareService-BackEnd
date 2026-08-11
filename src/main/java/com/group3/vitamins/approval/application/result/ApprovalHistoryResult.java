package com.group3.vitamins.approval.application.result;

import java.util.List;

/** 결재 이력조회(MGT-007) 결과 — 페이징 없이 전체 회차를 반환한다. */
public record ApprovalHistoryResult(List<ApprovalRevisionHistoryItem> content) {
}
