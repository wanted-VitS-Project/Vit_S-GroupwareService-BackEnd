package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceCsvUploadView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceDuplicateRowView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record TaxInvoiceCsvUploadResponse(
        @Schema(description = "전체 행 수", example = "5")
        int totalRows,
        @Schema(description = "저장 성공 건수", example = "4")
        int savedCount,
        @Schema(description = "중복으로 제외된 건수", example = "1")
        int duplicateCount,
        List<DuplicateRow> duplicateRows
) {

    public static TaxInvoiceCsvUploadResponse from(TaxInvoiceCsvUploadView view) {
        return new TaxInvoiceCsvUploadResponse(
                view.totalRows(), view.savedCount(), view.duplicateCount(),
                view.duplicateRows().stream().map(DuplicateRow::from).toList()
        );
    }

    public record DuplicateRow(
            @Schema(description = "중복 승인번호", example = "20260720-12345678")
            String approvalNo,
            @Schema(description = "제외 사유", example = "이미 등록된 승인번호입니다.")
            String reason
    ) {

        public static DuplicateRow from(TaxInvoiceDuplicateRowView view) {
            return new DuplicateRow(view.approvalNo(), view.reason());
        }
    }
}
