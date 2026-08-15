package com.group3.vitamins.finance.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record UpdateTaxInvoiceExclusionRequest(
        @Schema(description = "연결 제외 처리할 세금계산서 ID 목록", example = "[30, 31, 32]")
        List<Long> taxIds,
        @Schema(description = "제외 여부 (true: 제외, false: 제외 취소)", example = "true")
        Boolean isExcluded
) {
}
