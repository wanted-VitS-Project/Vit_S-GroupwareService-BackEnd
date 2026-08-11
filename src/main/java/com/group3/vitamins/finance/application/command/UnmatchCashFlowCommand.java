package com.group3.vitamins.finance.application.command;

public record UnmatchCashFlowCommand(
        Long cashFlowId,
        String userId,
        String role
) {
}
