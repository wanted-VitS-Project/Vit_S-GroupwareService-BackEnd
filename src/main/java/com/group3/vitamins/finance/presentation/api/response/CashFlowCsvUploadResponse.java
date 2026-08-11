package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.CashFlowCsvUploadView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.DuplicateRowView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CashFlowCsvUploadResponse(
        @Schema(description = "전체 행 수", example = "7")
        int totalRows,
        @Schema(description = "저장 성공 건수", example = "5")
        int savedCount,
        @Schema(description = "중복으로 제외된 건수", example = "2")
        int duplicateCount,
        List<DuplicateRow> duplicateRows
) {

    public static CashFlowCsvUploadResponse from(CashFlowCsvUploadView view) {
        return new CashFlowCsvUploadResponse(
                view.totalRows(), view.savedCount(), view.duplicateCount(),
                view.duplicateRows().stream().map(DuplicateRow::from).toList()
        );
    }

    public record DuplicateRow(
            @Schema(description = "중복 거래 일시", example = "2026-03-15T10:00:00")
            LocalDateTime tradedAt,
            @Schema(description = "중복 거래 금액", example = "500000")
            BigDecimal amount,
            @Schema(description = "제외 사유", example = "이미 등록된 거래입니다.")
            String reason
    ) {

        public static DuplicateRow from(DuplicateRowView view) {
            return new DuplicateRow(view.tradedAt(), view.amount(), view.reason());
        }
    }
}
