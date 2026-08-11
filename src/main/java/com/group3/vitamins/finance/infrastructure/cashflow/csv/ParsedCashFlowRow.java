package com.group3.vitamins.finance.infrastructure.cashflow.csv;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ParsedCashFlowRow(
        LocalDateTime tradedAt,
        String type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String depositorName,
        String bankMemo,
        String bankTxnId
) {
}
