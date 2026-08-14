package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.SkippedTaxInvoiceView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceExclusionResultView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record TaxInvoiceExclusionResponse(
        @Schema(description = "처리된 건수", example = "3")
        int updatedCount,
        @Schema(description = "처리되지 못한 항목 목록(제외 취소 요청이면 매칭 여부와 무관하게 항상 빈 배열)")
        List<SkippedItem> skippedItems
) {

    public static TaxInvoiceExclusionResponse from(TaxInvoiceExclusionResultView view) {
        return new TaxInvoiceExclusionResponse(
                view.updatedCount(), view.skippedItems().stream().map(SkippedItem::from).toList());
    }

    @Schema(name = "TaxInvoiceExclusionResponseSkippedItem")
    public record SkippedItem(
            @Schema(description = "처리되지 못한 세금계산서 ID", example = "31")
            Long taxId,
            @Schema(description = "처리되지 못한 사유", example = "이미 매칭된 항목은 제외 처리할 수 없습니다.")
            String reason
    ) {

        public static SkippedItem from(SkippedTaxInvoiceView view) {
            return new SkippedItem(view.taxId(), view.reason());
        }
    }
}
