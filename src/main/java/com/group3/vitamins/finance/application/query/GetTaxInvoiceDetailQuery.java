package com.group3.vitamins.finance.application.query;

public record GetTaxInvoiceDetailQuery(
        Long taxId,
        String userId,
        String role
) {
}
