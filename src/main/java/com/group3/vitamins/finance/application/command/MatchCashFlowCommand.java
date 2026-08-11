package com.group3.vitamins.finance.application.command;

public record MatchCashFlowCommand(
        Long cashFlowId,
        Long settleId,
        String userId,
        String role
) {
}
