package com.group3.vitamins.project.application.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectDetailResult(
        Long projectId,
        String name,
        String description,
        String clientName,
        String status,
        LocalDate startedOn,
        LocalDate endedOn,
        BigDecimal contractAmount,
        Integer progressRate,
        int stepCount,
        int doneStepCount,
        List<BusinessCategorySummary> businessCategories,
        Long bidNoticeId,
        String closeReasonCode,
        String closeReasonNote,
        String myPermission,
        LocalDateTime createdAt
) {
}