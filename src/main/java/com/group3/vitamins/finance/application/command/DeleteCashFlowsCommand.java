package com.group3.vitamins.finance.application.command;

import java.util.List;

public record DeleteCashFlowsCommand(
        List<Long> cashFlowIds,
        String userId,
        String role
) {
}
