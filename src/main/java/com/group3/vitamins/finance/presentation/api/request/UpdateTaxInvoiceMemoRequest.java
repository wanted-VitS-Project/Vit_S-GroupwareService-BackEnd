package com.group3.vitamins.finance.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateTaxInvoiceMemoRequest(
        @Schema(description = "비고/메모. null 이나 빈 문자열을 보내면 메모를 지운다", example = "재입고 관련 확인 필요")
        String memo
) {
}
