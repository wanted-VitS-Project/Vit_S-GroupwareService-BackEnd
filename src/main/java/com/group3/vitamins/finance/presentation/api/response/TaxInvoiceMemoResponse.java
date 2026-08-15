package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceMemoView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record TaxInvoiceMemoResponse(
        @Schema(description = "세금계산서 ID", example = "1")
        Long taxId,
        @Schema(description = "수정된 메모", example = "재입고 관련 확인 필요", nullable = true)
        String memo,
        @Schema(description = "수정일시", example = "2026-08-07T16:30:00")
        LocalDateTime updatedAt
) {

    public static TaxInvoiceMemoResponse from(TaxInvoiceMemoView view) {
        return new TaxInvoiceMemoResponse(view.taxId(), view.memo(), view.updatedAt());
    }
}
