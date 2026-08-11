package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.FinanceSummaryView;
import io.swagger.v3.oas.annotations.media.Schema;

public record FinanceSummaryResponse(
        CashFlowSummary cashFlow,
        TaxInvoiceSummary taxInvoice,
        SettlementSummary settlement
) {

    public static FinanceSummaryResponse from(FinanceSummaryView view) {
        return new FinanceSummaryResponse(
                new CashFlowSummary(view.cashFlowUnlinkedCount(), view.cashFlowTotalCount()),
                new TaxInvoiceSummary(view.taxInvoiceUnlinkedCount(), view.taxInvoiceTotalCount()),
                new SettlementSummary(view.settlementUnlinkedCount(), view.settlementInProgressCount())
        );
    }

    public record CashFlowSummary(
            @Schema(description = "정산 블록과 연결되지 않은 입출금 내역 건수", example = "3")
            long unlinkedCount,
            @Schema(description = "입출금 내역 전체 건수", example = "7")
            long totalCount
    ) {
    }

    public record TaxInvoiceSummary(
            @Schema(description = "정산 블록과 연결되지 않은 세금계산서 건수", example = "2")
            long unlinkedCount,
            @Schema(description = "세금계산서 전체 건수", example = "5")
            long totalCount
    ) {
    }

    public record SettlementSummary(
            @Schema(description = "연결되지 않은(미연결) 정산 블록 개수", example = "5")
            long unlinkedCount,
            @Schema(description = "상태가 완료·종료가 아닌 진행 중 프로젝트 개수", example = "3")
            long inProgressCount
    ) {
    }
}
