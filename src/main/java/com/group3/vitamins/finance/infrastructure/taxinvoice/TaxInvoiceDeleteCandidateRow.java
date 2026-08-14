package com.group3.vitamins.finance.infrastructure.taxinvoice;

/**
 * 삭제·연결 제외 처리 대상 확인용 — {@code settleBlockId}가 null 이 아니면 이미 매칭된 항목이다.
 * cash_flow의 CashFlowDeleteCandidateRow와 동일한 용도다.
 */
public record TaxInvoiceDeleteCandidateRow(
        Long taxId,
        Long settleBlockId
) {
}
