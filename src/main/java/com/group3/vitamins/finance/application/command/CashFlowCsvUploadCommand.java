package com.group3.vitamins.finance.application.command;

public record CashFlowCsvUploadCommand(
        byte[] fileBytes,
        String fileName,
        String password,
        String bankName,
        String dateTimeMode,
        String tradedDateTimeColumn,
        String tradedDateColumn,
        String tradedTimeColumn,
        String amountMode,
        String amountColumn,
        String typeColumn,
        String incomeAmountColumn,
        String outcomeAmountColumn,
        String depositorColumn,
        String memoColumn,
        String balanceColumn,
        String userId,
        String role
) {
}
