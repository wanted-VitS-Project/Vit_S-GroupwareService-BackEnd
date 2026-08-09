package com.group3.vitamins.settlement.application.query;

public record SettlementProjectBlockListQuery(
        Long projectId,
        String userId,
        String role
) {
}
