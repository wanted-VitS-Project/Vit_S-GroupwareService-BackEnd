package com.group3.vitamins.project.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateProjectCommand(
        Long bidNoticeId,
        String name,
        String description,
        String clientName,
        LocalDate startedOn,
        LocalDate endedOn,
        BigDecimal contractAmount,
        List<Long> businessCategoryIds,
        String requesterUserId
) {
}