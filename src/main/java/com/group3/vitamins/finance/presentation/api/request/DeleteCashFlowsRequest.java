package com.group3.vitamins.finance.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record DeleteCashFlowsRequest(
        @Schema(description = "삭제할 입출금 내역 ID 목록", example = "[1, 2, 3]")
        List<Long> cashFlowIds
) {
}
