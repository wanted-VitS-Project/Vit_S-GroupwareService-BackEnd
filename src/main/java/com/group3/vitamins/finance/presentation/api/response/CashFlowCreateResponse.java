package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.CashFlowDetailView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashFlowCreateResponse(
        @Schema(description = "생성된 입출금 내역 ID", example = "20")
        Long cashFlowId,
        @Schema(description = "거래고유번호 (은행명+거래일시 기반 자동생성)", example = "신한-20260807143000")
        String bankTxnId,
        @Schema(description = "은행명", example = "신한은행")
        String bankName,
        @Schema(description = "거래일시", example = "2026-08-07T14:30:00")
        LocalDateTime tradedAt,
        @Schema(description = "구분", example = "INCOME")
        String type,
        @Schema(description = "거래금액", example = "5000000")
        BigDecimal amount,
        @Schema(description = "입금자명/수취인명", example = "(주)테스트기업")
        String depositorName,
        @Schema(description = "적요/메모", example = "계약금 입금", nullable = true)
        String memo,
        @Schema(description = "수집 출처 (MANUAL 고정)", example = "MANUAL")
        String sourceType,
        @Schema(description = "생성일시", example = "2026-08-07T15:00:00")
        LocalDateTime createdAt
) {

    public static CashFlowCreateResponse from(CashFlowDetailView view) {
        return new CashFlowCreateResponse(
                view.cashFlowId(), view.bankTxnId(), view.bankName(), view.tradedAt(), view.type(), view.amount(),
                view.depositorName(), view.memo(), view.sourceType(), view.createdAt()
        );
    }
}
