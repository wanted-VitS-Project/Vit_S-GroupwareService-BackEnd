package com.group3.vitamins.project.block.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SETTLEMENT 블록 상세 — 정산 항목 작성/수정 API(`.ai/api/settlement.md`) 응답과 동일한 필드를 담는다.
 * {@code accountNumber} 는 이미 마스킹된 값이다 — 이 타입에는 복호화되지 않은 원문이 절대 담기지 않는다.
 * {@code paidAmountRatio} 는 이 블록 하나가 아니라 같은 프로젝트·같은 타입(INCOME/OUTCOME) 정산 블록
 * 전체의 실제 금액 합계를 이 타입의 프로젝트 총 예정 금액({@code totalAmount})으로 나눈 값이다.
 */
public record SettlementDetail(
        Long settleId,
        Integer roundNo,
        String type,
        String status,
        Long totalAmount,
        Long plannedAmount,
        Long plannedTaxAmount,
        LocalDate plannedDate,
        LocalDate taxInvoiceDueDate,
        // 이 정산 블록에 세금계산서가 연결됐는지 — status로는 판정할 수 없어 tax_invoice를 직접 본다
        // (PARTIAL/COMPLETED는 '입출금이 붙었다'만 말해준다, 2026-08-14 프론트 요청).
        Boolean taxInvoiceLinked,
        String traderName,
        String bankName,
        String accountNumber,
        String accountHolder,
        Long actualAmount,
        LocalDateTime actualDate,
        Double paidAmountRatio,
        LocalDateTime createdAt,
        int version
) implements BlockDetail {
}
