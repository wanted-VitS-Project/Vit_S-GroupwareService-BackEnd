package com.group3.vitamins.finance.application.command;

import java.util.List;

public record UpdateTaxInvoiceExclusionCommand(
        List<Long> taxIds,
        Boolean isExcluded,
        String userId,
        String role
) {
}
