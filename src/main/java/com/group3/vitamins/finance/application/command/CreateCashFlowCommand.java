package com.group3.vitamins.finance.application.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCashFlowCommand(
        String bankName,
        LocalDateTime tradedAt,
        String type,
        BigDecimal amount,
        String depositorName,
        String memo,
        String userId,
        String role
) {
}
