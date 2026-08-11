package com.group3.vitamins.finance.application.command;

public record CashFlowCsvPreviewCommand(
        byte[] fileBytes,
        String fileName,
        String password,
        String userId,
        String role
) {
}
