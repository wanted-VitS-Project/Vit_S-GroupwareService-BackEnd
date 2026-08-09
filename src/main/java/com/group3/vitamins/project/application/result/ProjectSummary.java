package com.group3.vitamins.project.application.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProjectSummary(
        Long projectId,
        String name,
        String clientName,
        String status,
        LocalDate startedOn,
        LocalDate endedOn,
        BigDecimal contractAmount,
        Integer progressRate,
        List<BusinessCategorySummary> businessCategories,
        List<MemberBrief> members,
        int myIssueInProgressCount,
        int myApprovalInProgressCount
) {
}