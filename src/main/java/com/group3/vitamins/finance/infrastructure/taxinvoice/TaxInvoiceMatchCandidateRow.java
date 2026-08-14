package com.group3.vitamins.finance.infrastructure.taxinvoice;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 세금계산서 매칭 추천 후보 한 행. {@code amountMatchType}/{@code taxAmountMatchType}/{@code traderMatchType}/
 * {@code dateMatchType}은 각각 {@code "EXACT"}/{@code "SIMILAR"}/{@code null}(불일치) — 쿼리에서 판정하고,
 * 태그 문자열(예: "금액 일치")로 바꾸는 건 서비스 계층이 한다. cash_flow의 MatchCandidateRow와 동일 사고방식
 * (세액 기준을 추가하고, 일자는 발행일 대비 약한 신호로만 둔다 — 2026-08-13 사용자 확인).
 */
public record TaxInvoiceMatchCandidateRow(
        Long settleId,
        String roundName,
        String projectName,
        BigDecimal plannedAmount,
        BigDecimal plannedTaxAmount,
        LocalDate plannedDate,
        String traderName,
        String amountMatchType,
        String taxAmountMatchType,
        String traderMatchType,
        String dateMatchType
) {
}
