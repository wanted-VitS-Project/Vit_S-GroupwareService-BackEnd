package com.group3.vitamins.settlement.infrastructure.blockdetail;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * settlement_block 조회 행. MyBatis 가 생성자로 매핑한다.
 * {@code accountNumber} 는 암호화된 원문 그대로다 — 마스킹은 어댑터가 복호화 후 처리한다.
 *
 * <p>{@code actualAmountSum} 은 이 행 하나의 실제 금액이 아니라, **같은 프로젝트 · 같은 타입**(INCOME/OUTCOME)의
 * 활성 정산 블록 전체에 걸친 {@code actual_amount} 합계다 — SQL의 JOIN·GROUP BY 로 미리 계산해서 내려온다.
 * 진행률(paidAmountRatio) = actualAmountSum / totalAmount 로 계산한다(어댑터 책임).
 */
public record SettlementDetailRow(
        Long settleId,
        Integer roundNo,
        String type,
        String status,
        Long totalAmount,
        Long plannedAmount,
        Long plannedTaxAmount,
        LocalDate plannedDate,
        LocalDate taxInvoiceDueDate,
        Boolean taxInvoiceLinked,
        String traderName,
        String bankName,
        String accountNumber,
        String accountHolder,
        Long actualAmount,
        LocalDateTime actualDate,
        LocalDateTime createdAt,
        int version,
        Long actualAmountSum
) {
}
