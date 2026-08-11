package com.group3.vitamins.finance.infrastructure.cashflow.csv;

public record CashFlowCsvRecommendation(
        CashFlowDateTimeMode dateTimeMode,
        CashFlowAmountMode amountMode,
        CashFlowCsvMapping mapping
) {
}
