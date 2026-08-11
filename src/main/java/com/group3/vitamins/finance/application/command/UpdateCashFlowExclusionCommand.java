package com.group3.vitamins.finance.application.command;

import java.util.List;

public record UpdateCashFlowExclusionCommand(
        List<Long> cashFlowIds,
        Boolean isExcluded,
        String userId,
        String role
) {
}
