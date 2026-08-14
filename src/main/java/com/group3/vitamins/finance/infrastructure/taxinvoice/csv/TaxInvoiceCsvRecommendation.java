package com.group3.vitamins.finance.infrastructure.taxinvoice.csv;

/**
 * @param recommendedType 헤더 위 제목 줄(예: "2022년도 매출세금계산서")의 "매출"/"매입" 키워드로 판단한
 *                        추천 구분 — "매출"이면 INCOME, "매입"이면 OUTCOME. 제목 줄이 없거나 두 키워드가
 *                        다 없으면 null(사용자가 직접 라디오 버튼으로 선택).
 *                        ⚠️ 사업자번호 값으로 판단하던 옛 방식이 아니다(2026-08-13 정정) — 실제 세금계산서는
 *                        공급자·공급받는자 사업자번호가 둘 다 항상 채워져 있어 그 기준으로는 구분이 안 됐다.
 *                        판정 구현은 {@link TaxInvoiceCsvColumnRecommender#recommendType} 참고.
 */
public record TaxInvoiceCsvRecommendation(String recommendedType, TaxInvoiceCsvMapping mapping) {
}
