package com.group3.vitamins.finance.application.query;

public record MatchCandidatesQuery(
        Long cashFlowId,
        String userId,
        String role
) {
}
