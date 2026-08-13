package com.group3.vitamins.finance.infrastructure.taxinvoice.csv;

import java.util.List;
import java.util.Map;

/**
 * 파싱된 CSV 전체 — 헤더(컬럼명 목록)와 전체 데이터 행(컬럼명 → 셀 값). 빈 셀은 빈 문자열이 아니라
 * {@code null}로 정규화해서 담는다(cash_flow의 CashFlowCsvTable과 동일 사고방식).
 *
 * <p>{@code titleText}는 헤더 판정용으로만 훑고 버려지던 헤더 위 제목 줄(예: "2022년도 매출세금계산서")의
 * 텍스트를 그대로 담은 것 — recommendedType 추천(매출/매입 키워드 매칭)에 쓴다. 제목 줄이 없으면 null.
 */
public record TaxInvoiceCsvTable(
        List<String> headers,
        List<Map<String, String>> rows,
        String titleText
) {
}
