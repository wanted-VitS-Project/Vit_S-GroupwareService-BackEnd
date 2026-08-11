package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.CashFlowDeleteResultView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.SkippedCashFlowView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CashFlowDeleteResponse(
        @Schema(description = "실제 삭제된 건수", example = "2")
        int deletedCount,
        @Schema(description = "매칭돼 있거나 존재하지 않아 삭제되지 못한 항목 목록")
        List<SkippedItem> skippedItems
) {

    public static CashFlowDeleteResponse from(CashFlowDeleteResultView view) {
        return new CashFlowDeleteResponse(
                view.deletedCount(), view.skippedItems().stream().map(SkippedItem::from).toList());
    }

    @Schema(name = "CashFlowDeleteResponseSkippedItem")
    public record SkippedItem(
            @Schema(description = "삭제하지 못한 입출금 내역 ID", example = "3")
            Long cashFlowId,
            @Schema(description = "삭제하지 못한 사유", example = "매칭된 항목은 삭제할 수 없습니다. 먼저 매칭을 해제해주세요.")
            String reason
    ) {

        public static SkippedItem from(SkippedCashFlowView view) {
            return new SkippedItem(view.cashFlowId(), view.reason());
        }
    }
}
