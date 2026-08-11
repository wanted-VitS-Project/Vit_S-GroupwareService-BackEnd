package com.group3.vitamins.finance.infrastructure.status;

/**
 * 재무 관리 요약 조회 — 스칼라 서브쿼리 6개를 한 번에 묶어 항상 단일 행으로 돌아온다.
 *
 * @param cashFlowUnlinkedCount   입출금 내역 중 제외 대상이 아니면서 정산 블록에 안 붙은 건수
 * @param cashFlowTotalCount      입출금 내역 전체 건수(제외 여부 무관, 소프트 삭제만 제외)
 * @param taxInvoiceUnlinkedCount 세금계산서 중 제외 대상이 아니면서 정산 블록에 안 붙은 건수
 * @param taxInvoiceTotalCount    세금계산서 전체 건수(제외 여부 무관, 소프트 삭제만 제외)
 * @param settlementUnlinkedCount status='PENDING'(미연결)인 활성 정산 블록 개수
 * @param settlementInProgressCount 상태가 COMPLETED·CLOSED가 아닌 활성 프로젝트 개수
 */
public record FinanceSummaryRow(
        long cashFlowUnlinkedCount,
        long cashFlowTotalCount,
        long taxInvoiceUnlinkedCount,
        long taxInvoiceTotalCount,
        long settlementUnlinkedCount,
        long settlementInProgressCount
) {
}
