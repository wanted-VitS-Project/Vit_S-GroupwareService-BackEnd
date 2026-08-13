package com.group3.vitamins.finance.application.command;

import java.util.List;

public record DeleteTaxInvoicesCommand(
        List<Long> taxIds,
        String userId,
        String role
) {
}
