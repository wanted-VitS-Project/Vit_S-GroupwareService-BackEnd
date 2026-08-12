package com.group3.vitamins.finance.infrastructure.taxinvoice.csv;

/**
 * @param recommendedType 공급자/공급받는자 사업자번호 중 실제 값이 채워진 쪽으로 판단한 추천 구분
 *                         (없으면 null — 사용자가 직접 라디오 버튼으로 선택)
 */
public record TaxInvoiceCsvRecommendation(String recommendedType, TaxInvoiceCsvMapping mapping) {
}
