package com.group3.vitamins.settlement.presentation.api.response;

import com.group3.vitamins.settlement.application.usecase.SettlementQueryUseCase.SettlementRecommendationView;
import io.swagger.v3.oas.annotations.media.Schema;

public record SettlementRecommendationResponse(
        @Schema(description = "수정할 정산 블록 ID", example = "1")
        Long settleId,

        @Schema(description = "같은 프로젝트 내 정산 블록 개수 기반으로 추천된 회차 번호", example = "2")
        Integer recommendRoundNo,

        @Schema(description = "같은 프로젝트 내 다른 정산 블록의 총 예정 금액 기반으로 추천된 값. "
                + "추천할 값이 없으면(같은 프로젝트에 값이 채워진 블록이 아직 없으면) null", example = "4500000", nullable = true)
        Long recommendTotalAmount,

        @Schema(description = "마스킹 없는 원본 계좌번호. 이 블록의 타입이 OUTCOME인 경우에만 값이 있다",
                example = "100555574444", nullable = true)
        String originalAccountNumber
) {

    public static SettlementRecommendationResponse from(SettlementRecommendationView view) {
        return new SettlementRecommendationResponse(
                view.settleId(),
                view.recommendedRoundNo(),
                view.recommendedTotalAmount(),
                view.originalAccountNumber()
        );
    }
}
