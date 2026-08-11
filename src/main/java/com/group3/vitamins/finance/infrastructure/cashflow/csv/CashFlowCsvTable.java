package com.group3.vitamins.finance.infrastructure.cashflow.csv;

import java.util.List;
import java.util.Map;

/**
 * 파싱된 CSV 전체 — 헤더(컬럼명 목록)와 전체 데이터 행(컬럼명 → 셀 값). 빈 셀은 빈 문자열이 아니라
 * {@code null}로 정규화해서 담는다(모드 판정·필수값 체크에서 빈 문자열/공백 구분을 신경 안 써도 되게).
 */
public record CashFlowCsvTable(
        List<String> headers,
        List<Map<String, String>> rows
) {
}
