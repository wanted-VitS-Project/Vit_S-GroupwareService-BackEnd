package com.group3.vitamins.finance.presentation.api.request;

import com.group3.vitamins.finance.application.command.UpdateCashFlowCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 전부 선택(부분 수정). 직접 등록(MANUAL)·미매칭 항목만 memo 외 필드가 반영된다 — 그 외는 memo만 허용. */
public record UpdateCashFlowRequest(
        @Schema(description = "은행명 (직접등록·미매칭 항목만 반영)", nullable = true)
        String bankName,
        @Schema(description = "거래일시 (직접등록·미매칭 항목만 반영)", nullable = true)
        LocalDateTime tradedAt,
        @Schema(description = "구분 INCOME/OUTCOME (직접등록·미매칭 항목만 반영)", nullable = true)
        String type,
        @Schema(description = "거래금액 (직접등록·미매칭 항목만 반영)", nullable = true)
        BigDecimal amount,
        @Schema(description = "입금자명/수취인명 (직접등록·미매칭 항목만 반영)", nullable = true)
        String depositorName,
        @Schema(description = "적요/메모 (모든 항목에 적용)", example = "선급금(수정됨)", nullable = true)
        String memo
) {

    public UpdateCashFlowCommand toCommand(Long cashFlowId, String userId, String role) {
        return new UpdateCashFlowCommand(cashFlowId, bankName, tradedAt, type, amount, depositorName, memo, userId, role);
    }
}
