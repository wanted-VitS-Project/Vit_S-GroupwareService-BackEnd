package com.group3.vitamins.finance.application.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 필드는 전부 선택(부분 수정) — null이면 "안 보낸 것"으로 취급한다. */
public record UpdateCashFlowCommand(
        Long cashFlowId,
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
