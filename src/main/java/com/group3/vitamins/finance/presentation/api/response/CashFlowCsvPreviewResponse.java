package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.CashFlowCsvMappingView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.CashFlowCsvPreviewView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public record CashFlowCsvPreviewResponse(
        @Schema(description = "CSV에 있는 전체 컬럼명 목록", example = "[\"날짜\", \"출금금액\", \"입금금액\", \"잔액\", \"적요\"]")
        List<String> columns,
        @Schema(description = "은행명 선택 드롭다운에 넣을 은행 목록")
        List<String> bankOptions,
        @Schema(description = "상위 5행 미리보기 (컬럼명: 값)")
        List<Map<String, String>> sampleRows,
        @Schema(description = "추천 일시 입력 방식 (SINGLE: 거래일시/SEPARATE: 거래일자+거래시간)", example = "SINGLE")
        String recommendedDateTimeMode,
        @Schema(description = "추천 금액 입력 방식 (SINGLE_WITH_TYPE: 금액+입출금/SEPARATE: 입금액+출금액)", example = "SEPARATE")
        String recommendedAmountMode,
        RecommendedMapping recommendedMapping
) {

    public static CashFlowCsvPreviewResponse from(CashFlowCsvPreviewView view) {
        return new CashFlowCsvPreviewResponse(
                view.columns(),
                view.bankOptions(),
                view.sampleRows(),
                view.recommendedDateTimeMode(),
                view.recommendedAmountMode(),
                RecommendedMapping.from(view.recommendedMapping())
        );
    }

    public record RecommendedMapping(
            @Schema(description = "통합 일시 컬럼 추천값 (없으면 null)", example = "날짜", nullable = true)
            String tradedDateTimeColumn,
            @Schema(description = "거래일자 컬럼 추천값 (없으면 null)", nullable = true)
            String tradedDateColumn,
            @Schema(description = "거래시간 컬럼 추천값 (없으면 null)", nullable = true)
            String tradedTimeColumn,
            @Schema(description = "단일 금액 컬럼 추천값 (없으면 null)", nullable = true)
            String amountColumn,
            @Schema(description = "구분(입출금) 컬럼 추천값 (없으면 null)", nullable = true)
            String typeColumn,
            @Schema(description = "입금액 컬럼 추천값 (없으면 null)", example = "입금금액", nullable = true)
            String incomeAmountColumn,
            @Schema(description = "출금액 컬럼 추천값 (없으면 null)", example = "출금금액", nullable = true)
            String outcomeAmountColumn,
            @Schema(description = "적요 컬럼 추천값 (없으면 null)", example = "적요", nullable = true)
            String memoColumn,
            @Schema(description = "입금자명 컬럼 추천값 (없으면 null)", nullable = true)
            String depositorColumn,
            @Schema(description = "잔액 컬럼 추천값 (없으면 null) — 원 명세엔 없던 필드(2026-08-10 추가)",
                    example = "잔액", nullable = true)
            String balanceColumn
    ) {

        public static RecommendedMapping from(CashFlowCsvMappingView view) {
            return new RecommendedMapping(
                    view.tradedDateTimeColumn(), view.tradedDateColumn(), view.tradedTimeColumn(),
                    view.amountColumn(), view.typeColumn(),
                    view.incomeAmountColumn(), view.outcomeAmountColumn(),
                    view.memoColumn(), view.depositorColumn(), view.balanceColumn()
            );
        }
    }
}
