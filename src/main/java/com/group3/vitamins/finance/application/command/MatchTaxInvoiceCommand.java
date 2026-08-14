package com.group3.vitamins.finance.application.command;

public record MatchTaxInvoiceCommand(
        Long taxId,
        Long settleId,
        String userId,
        String role
) {
}
