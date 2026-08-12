package com.group3.vitamins.finance.infrastructure.taxinvoice.csv;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * CSV 헤더명을 키워드로 매칭해 컬럼 매핑 + 구분(INCOME/OUTCOME)을 추천한다. cash_flow의
 * CashFlowCsvColumnRecommender와 동일한 사고방식(키워드 사전 기반, 추천이 틀려도 사용자가 화면에서
 * 직접 고쳐 확정하므로 최종 저장 값에는 영향 없음).
 */
@Component
public class TaxInvoiceCsvColumnRecommender {

    private static final List<String> APPROVAL_NO_KEYWORDS = List.of("승인번호");
    private static final List<String> ISSUED_DATE_KEYWORDS = List.of("작성일자", "발행일", "작성일");
    private static final List<String> SUPPLIER_BIZ_NO_KEYWORDS = List.of("공급자사업자번호", "공급자사업자등록번호", "공급자");
    private static final List<String> BUYER_BIZ_NO_KEYWORDS = List.of("공급받는자사업자번호", "공급받는자사업자등록번호", "공급받는자");
    private static final List<String> BUYER_NAME_KEYWORDS = List.of("상호", "거래처", "공급받는자상호");
    private static final List<String> SUPPLY_AMOUNT_KEYWORDS = List.of("공급가액");
    private static final List<String> TAX_AMOUNT_KEYWORDS = List.of("세액");
    private static final List<String> TOTAL_AMOUNT_KEYWORDS = List.of("합계금액", "합계");
    private static final List<String> ITEM_NAME_KEYWORDS = List.of("품목명", "품목");
    private static final List<String> CEO_NAME_KEYWORDS = List.of("대표자명", "대표자");
    private static final List<String> SUB_BIZ_NO_KEYWORDS = List.of("종사업장번호", "종사업장");
    private static final List<String> MEMO_KEYWORDS = List.of("비고", "메모");

    public TaxInvoiceCsvRecommendation recommend(List<String> headers, List<Map<String, String>> rows) {
        String supplierBizNoColumn = findFirst(headers, SUPPLIER_BIZ_NO_KEYWORDS);
        String buyerBizNoColumn = findFirst(headers, BUYER_BIZ_NO_KEYWORDS);

        TaxInvoiceCsvMapping mapping = new TaxInvoiceCsvMapping(
                findFirst(headers, APPROVAL_NO_KEYWORDS),
                findFirst(headers, ISSUED_DATE_KEYWORDS),
                supplierBizNoColumn,
                buyerBizNoColumn,
                findFirst(headers, BUYER_NAME_KEYWORDS),
                findFirst(headers, SUPPLY_AMOUNT_KEYWORDS),
                findFirst(headers, TAX_AMOUNT_KEYWORDS),
                findFirst(headers, TOTAL_AMOUNT_KEYWORDS),
                findFirst(headers, ITEM_NAME_KEYWORDS),
                findFirst(headers, CEO_NAME_KEYWORDS),
                findFirst(headers, SUB_BIZ_NO_KEYWORDS),
                findFirst(headers, MEMO_KEYWORDS)
        );

        return new TaxInvoiceCsvRecommendation(recommendType(rows, supplierBizNoColumn, buyerBizNoColumn), mapping);
    }

    /**
     * 사용자 확인(2026-08-12) — 공급자 사업자번호 컬럼에 실제 값이 채워져 있으면 매입(OUTCOME),
     * 공급받는자 사업자번호 컬럼에 값이 채워져 있으면 매출(INCOME)로 추천한다. 첫 데이터 행만 본다
     * (추천은 어차피 비확정값 — 사용자가 라디오 버튼으로 최종 확정).
     */
    private String recommendType(List<Map<String, String>> rows, String supplierBizNoColumn, String buyerBizNoColumn) {
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, String> firstRow = rows.get(0);
        if (supplierBizNoColumn != null && firstRow.get(supplierBizNoColumn) != null) {
            return "OUTCOME";
        }
        if (buyerBizNoColumn != null && firstRow.get(buyerBizNoColumn) != null) {
            return "INCOME";
        }
        return null;
    }

    private String findFirst(List<String> headers, List<String> keywords) {
        for (String keyword : keywords) {
            for (String header : headers) {
                if (header.contains(keyword)) {
                    return header;
                }
            }
        }
        return null;
    }
}
