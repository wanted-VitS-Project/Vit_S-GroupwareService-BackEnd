package com.group3.vitamins.finance.presentation.api.request;

import com.group3.vitamins.finance.application.command.CreateCashFlowCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCashFlowRequest(
        @Schema(description = "은행명", example = "신한은행")
        String bankName,
        @Schema(description = "거래일시", example = "2026-08-07T14:30:00")
        LocalDateTime tradedAt,
        @Schema(description = "구분 (INCOME/OUTCOME)", example = "INCOME")
        String type,
        @Schema(description = "거래금액", example = "5000000")
        BigDecimal amount,
        @Schema(description = "입금자명/수취인명", example = "(주)테스트기업")
        String depositorName,
        @Schema(description = "적요/메모", example = "계약금 입금", nullable = true)
        String memo
) {

    public CreateCashFlowCommand toCommand(String userId, String role) {
        return new CreateCashFlowCommand(bankName, tradedAt, type, amount, depositorName, memo, userId, role);
    }
}
