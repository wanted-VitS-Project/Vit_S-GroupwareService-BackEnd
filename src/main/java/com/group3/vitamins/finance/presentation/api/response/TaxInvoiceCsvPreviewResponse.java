package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceCsvMappingView;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase.TaxInvoiceCsvPreviewView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public record TaxInvoiceCsvPreviewResponse(
        @Schema(description = "CSV에 있는 전체 컬럼명 목록. 같은 이름이 여러 번 나오면(공급자/공급받는자 블록 각각의 "
                + "\"상호\"·\"대표자명\"·\"종사업장번호\") 두 번째부터 \" (2)\", \" (3)\" 접미사가 붙어 구분된다")
        List<String> columns,
        @Schema(description = "상위 5행 미리보기 (컬럼명: 값)")
        List<Map<String, String>> sampleRows,
        @Schema(description = "추천 구분 (INCOME/OUTCOME) — 헤더 위 제목 줄(예: \"2022년도 매출세금계산서\")에 "
                + "\"매출\"이 있으면 INCOME, \"매입\"이 있으면 OUTCOME. 제목 줄이 없거나 두 키워드가 다 없으면 null "
                + "(사용자가 직접 선택해야 한다)", example = "INCOME", nullable = true)
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
            // 중복 헤더가 있는 파일이면 방향에 따라 " (2)"가 붙은 쪽이 추천된다(매출이면 공급받는자 쪽) —
            // example은 접미사 없는 형태로 둔다.
            @Schema(description = "대표자명 컬럼 추천값 (없으면 null)", example = "대표자명", nullable = true)
            String ceoNameColumn,
            @Schema(description = "종사업장번호 컬럼 추천값 (없으면 null)", example = "종사업장번호", nullable = true)
            String subBizNoColumn,
            @Schema(description = "비고/메모 컬럼 추천값 (없으면 null)", example = "비고", nullable = true)
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
