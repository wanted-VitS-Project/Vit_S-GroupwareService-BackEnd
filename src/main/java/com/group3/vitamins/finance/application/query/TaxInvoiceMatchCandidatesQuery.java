package com.group3.vitamins.finance.application.query;

public record TaxInvoiceMatchCandidatesQuery(
        Long taxId,
        String userId,
        String role
) {
}
