package com.group3.vitamins.finance.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record DeleteTaxInvoicesRequest(
        @Schema(description = "삭제할 세금계산서 ID 목록", example = "[1, 2, 3]")
        List<Long> taxIds
) {
}
