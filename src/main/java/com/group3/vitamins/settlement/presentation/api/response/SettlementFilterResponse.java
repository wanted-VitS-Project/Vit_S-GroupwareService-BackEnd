package com.group3.vitamins.settlement.presentation.api.response;

import com.group3.vitamins.settlement.application.usecase.SettlementQueryUseCase.SettlementFilterView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SettlementFilterResponse(
        @Schema(description = "정산 현황에 등장하는 발주처 목록", example = "[\"환경부\", \"국토교통부\"]")
        List<String> clients
) {

    public static SettlementFilterResponse from(SettlementFilterView view) {
        return new SettlementFilterResponse(view.clients());
    }
}
