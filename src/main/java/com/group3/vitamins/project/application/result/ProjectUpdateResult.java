package com.group3.vitamins.project.application.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectUpdateResult(
        Long projectId,
        String name,
        String clientName,
        LocalDate startedOn,
        LocalDate endedOn,
        BigDecimal contractAmount,
        LocalDateTime updatedAt
) {
}