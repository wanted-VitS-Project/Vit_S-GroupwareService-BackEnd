package com.group3.vitamins.finance.application.command;

public record UnmatchTaxInvoiceCommand(
        Long taxId,
        String userId,
        String role
) {
}
