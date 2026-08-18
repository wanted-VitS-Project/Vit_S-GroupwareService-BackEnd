package com.group3.vitamins.finance.application.query;

public record GetCashFlowDetailQuery(
        Long cashFlowId,
        String userId,
        String role
) {
}
