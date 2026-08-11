package com.group3.vitamins.finance.infrastructure.cashflow;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 입출금 내역 매칭 추천 후보 한 행. {@code amountMatchType}/{@code dateMatchType}/{@code traderMatchType}은
 * 각각 {@code "EXACT"}/{@code "SIMILAR"}/{@code null}(불일치) — 쿼리에서 판정하고, 태그 문자열
 * (예: "금액 일치")로 바꾸는 건 서비스 계층이 한다.
 */
public record MatchCandidateRow(
        Long settleId,
        String roundName,
        String projectName,
        BigDecimal plannedAmount,
        LocalDate plannedDate,
        String traderName,
        String amountMatchType,
        String dateMatchType,
        String traderMatchType
) {
}
