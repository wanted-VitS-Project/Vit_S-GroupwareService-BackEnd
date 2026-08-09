package com.group3.vitamins.settlement.infrastructure.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 정산 현황 블록 조회용 회차별 행.
 *
 * @param taxInvoiceAmount 정산 블록의 planned_tax_amount 그대로다 — tax_invoice 테이블 값이 아니다(요청사항)
 * @param taxLinkedBy 이 블록에 연결된 tax_invoice 중 가장 최근 것의 연결자 사번 (없으면 null)
 * @param cashFlowLinkedBy 이 블록에 연결된 cash_flow 중 가장 최근 것의 연결자 사번 (없으면 null)
 */
public record SettlementProjectBlockRow(
        Long settleId,
        Integer roundNo,
        String roundName,
        LocalDate plannedDate,
        Long plannedAmount,
        Long plannedTaxAmount,
        LocalDate taxInvoiceDate,
        Long taxInvoiceAmount,
        String paidType,
        String bankName,
        String accountNumber,
        String accountHolder,
        LocalDate paidDate,
        Long paidAmount,
        String status,
        String taxLinkedBy,
        String taxLinkedByName,
        LocalDateTime taxLinkedAt,
        String cashFlowLinkedBy,
        String cashFlowLinkedByName,
        LocalDateTime cashFlowLinkedAt
) {
}
