package com.group3.vitamins.finance.application.query;

import java.time.LocalDate;

public record CashFlowListQuery(
        LocalDate startDate,
        LocalDate endDate,
        Boolean unlinked,
        Long projectId,
        String keyword,
        String userId,
        String role
) {
}
