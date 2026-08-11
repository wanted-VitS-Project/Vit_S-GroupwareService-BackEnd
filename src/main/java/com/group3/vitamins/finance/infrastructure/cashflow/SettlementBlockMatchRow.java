package com.group3.vitamins.finance.infrastructure.cashflow;

import java.math.BigDecimal;

/** 매칭 대상 정산 블록 검증에 필요한 값. */
public record SettlementBlockMatchRow(
        String type,
        String status,
        BigDecimal plannedAmount
) {
}
