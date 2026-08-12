package com.group3.vitamins.settlement.application.query;

import java.time.LocalDate;

public record SettlementProjectListQuery(
        LocalDate startDate,
        LocalDate endDate,
        String client,
        Boolean includeCompleted,
        int page,
        int size,
        String sort,
        String userId,
        String role
) {
}
