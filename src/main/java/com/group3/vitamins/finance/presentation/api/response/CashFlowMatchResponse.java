package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.CashFlowMatchView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CashFlowMatchResponse(
        @Schema(description = "입출금 내역 ID", example = "5")
        Long cashFlowId,
        @Schema(description = "연결된 정산 블록 ID", example = "11")
        Long settleId,
        @Schema(description = "연결된 정산 블록명", example = "2차 기성(중도금)")
        String roundName,
        @Schema(description = "연결된 프로젝트명", example = "한강 생태교육 환경개선사업")
        String projectName,
        @Schema(description = "매칭 처리자 사번", example = "vitas-EMP004")
        String linkedBy,
        @Schema(description = "매칭 처리자 이름", example = "김재무")
        String linkedByName,
        @Schema(description = "매칭 일시", example = "2026-08-07T15:30:00")
        LocalDateTime linkedAt
) {

    public static CashFlowMatchResponse from(CashFlowMatchView view) {
        return new CashFlowMatchResponse(
                view.cashFlowId(), view.settleId(), view.roundName(), view.projectName(),
                view.linkedBy(), view.linkedByName(), view.linkedAt()
        );
    }
}
