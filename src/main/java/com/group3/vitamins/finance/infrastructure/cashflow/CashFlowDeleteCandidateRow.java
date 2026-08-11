package com.group3.vitamins.finance.infrastructure.cashflow;

/** 배치 삭제 대상 판정용 — 요청받은 ID 중 존재하는(삭제 안 된) 것만 돌아온다. */
public record CashFlowDeleteCandidateRow(
        Long cashFlowId,
        Long settleBlockId
) {
}
