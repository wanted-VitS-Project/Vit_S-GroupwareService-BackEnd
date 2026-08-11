package com.group3.vitamins.finance.infrastructure.cashflow;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashFlowDedupKeyRow(
        LocalDateTime tradedAt,
        BigDecimal amount,
        BigDecimal balanceAfter
) {
}
