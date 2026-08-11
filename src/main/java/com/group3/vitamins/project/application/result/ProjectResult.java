package com.group3.vitamins.project.application.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectResult(
        Long projectId,
        String name,
        String clientName,
        String status,
        LocalDate startedOn,
        LocalDate endedOn,
        BigDecimal contractAmount,
        List<BusinessCategorySummary> businessCategories,
        Long bidNoticeId,
        CreatedBy createdBy,
        LocalDateTime createdAt
) {
    public record CreatedBy(String userId, String name) {}
}