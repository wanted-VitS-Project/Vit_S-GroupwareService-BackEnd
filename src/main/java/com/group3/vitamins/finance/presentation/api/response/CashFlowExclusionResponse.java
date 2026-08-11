package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.CashFlowExclusionResultView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.SkippedCashFlowView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CashFlowExclusionResponse(
        @Schema(description = "처리된 건수", example = "3")
        int updatedCount,
        @Schema(description = "이미 매칭돼 있어 제외 처리되지 못한 항목 목록(제외 취소 요청이면 항상 빈 배열)")
        List<SkippedItem> skippedItems
) {

    public static CashFlowExclusionResponse from(CashFlowExclusionResultView view) {
        return new CashFlowExclusionResponse(
                view.updatedCount(), view.skippedItems().stream().map(SkippedItem::from).toList());
    }

    public record SkippedItem(
            @Schema(description = "제외 처리되지 못한 입출금 내역 ID", example = "31")
            Long cashFlowId,
            @Schema(description = "처리되지 못한 사유", example = "이미 매칭된 항목은 제외 처리할 수 없습니다.")
            String reason
    ) {

        public static SkippedItem from(SkippedCashFlowView view) {
            return new SkippedItem(view.cashFlowId(), view.reason());
        }
    }
}
