package com.group3.vitamins.finance.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record MatchCashFlowRequest(
        @Schema(description = "연결할 정산 블록 ID", example = "11")
        Long settleId
) {
}
