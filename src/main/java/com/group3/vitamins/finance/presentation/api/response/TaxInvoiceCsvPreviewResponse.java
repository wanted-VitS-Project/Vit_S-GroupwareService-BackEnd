package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceCsvMappingView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceCsvPreviewView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public record TaxInvoiceCsvPreviewResponse(
        @Schema(description = "CSV에 있는 전체 컬럼명 목록")
        List<String> columns,
        @Schema(description = "상위 5행 미리보기 (컬럼명: 값)")
        List<Map<String, String>> sampleRows,
        @Schema(description = "추천 구분 (INCOME/OUTCOME) — 공급자/공급받는자 사업자번호 중 실제 값이 채워진 쪽으로 판단. "
                + "판단 불가하면 null", example = "INCOME", nullable = true)
        String recommendedType,
        RecommendedMapping recommendedMapping
) {

    public static TaxInvoiceCsvPreviewResponse from(TaxInvoiceCsvPreviewView view) {
        return new TaxInvoiceCsvPreviewResponse(
                view.columns(), view.sampleRows(), view.recommendedType(),
                RecommendedMapping.from(view.recommendedMapping())
        );
    }

    public record RecommendedMapping(
            @Schema(description = "승인번호 컬럼 추천값 (없으면 null)", example = "승인번호", nullable = true)
            String approvalNoColumn,
            @Schema(description = "작성일자 컬럼 추천값 (없으면 null)", example = "작성일자", nullable = true)
            String issuedDateColumn,
            @Schema(description = "공급자 사업자번호 컬럼 추천값 (없으면 null)", example = "공급자사업자번호", nullable = true)
            String supplierBizNoColumn,
            @Schema(description = "공급받는자 사업자번호 컬럼 추천값 (없으면 null)", example = "공급받는자사업자번호", nullable = true)
            String buyerBizNoColumn,
            @Schema(description = "공급받는자 상호 컬럼 추천값 (없으면 null)", example = "상호", nullable = true)
            String buyerNameColumn,
            @Schema(description = "공급가액 컬럼 추천값 (없으면 null)", example = "공급가액", nullable = true)
            String supplyAmountColumn,
            @Schema(description = "세액 컬럼 추천값 (없으면 null)", example = "세액", nullable = true)
            String taxAmountColumn,
            @Schema(description = "합계금액 컬럼 추천값 (없으면 null)", example = "합계금액", nullable = true)
            String totalAmountColumn,
            @Schema(description = "품목명 컬럼 추천값 (없으면 null)", example = "품목", nullable = true)
            String itemNameColumn,
            @Schema(description = "대표자명 컬럼 추천값 (없으면 null)", nullable = true)
            String ceoNameColumn,
            @Schema(description = "종사업장번호 컬럼 추천값 (없으면 null)", nullable = true)
            String subBizNoColumn,
            @Schema(description = "비고/메모 컬럼 추천값 (없으면 null)", nullable = true)
            String memoColumn
    ) {

        public static RecommendedMapping from(TaxInvoiceCsvMappingView view) {
            return new RecommendedMapping(
                    view.approvalNoColumn(), view.issuedDateColumn(),
                    view.supplierBizNoColumn(), view.buyerBizNoColumn(), view.buyerNameColumn(),
                    view.supplyAmountColumn(), view.taxAmountColumn(), view.totalAmountColumn(),
                    view.itemNameColumn(), view.ceoNameColumn(), view.subBizNoColumn(), view.memoColumn()
            );
        }
    }
}
