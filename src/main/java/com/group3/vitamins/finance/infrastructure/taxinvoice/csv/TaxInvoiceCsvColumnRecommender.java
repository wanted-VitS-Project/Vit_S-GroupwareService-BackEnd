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

    public TaxInvoiceCsvRecommendation recommend(List<String> headers, String titleText) {
        String recommendedType = recommendType(titleText);
        // 매입(OUTCOME)이면 거래상대방(외주업체)이 공급자 쪽이라 그쪽 값을 골라야 한다. 매출(INCOME)이거나
        // 판단 불가하면 기존처럼 공급받는자 쪽(뒤에 나온 occurrence)을 우선한다(2026-08-13, 사용자 확인 —
        // "우리가 돈을 줘야 하면 외주업체 정보가, 돈을 받아야 하면 우리에게 돈을 주는 업체 정보가 저장돼야
        // 한다"는 지적으로 반영. 아래 findByDirection 참고).
        boolean preferSupplierSide = "OUTCOME".equals(recommendedType);

        TaxInvoiceCsvMapping mapping = new TaxInvoiceCsvMapping(
                findFirst(headers, APPROVAL_NO_KEYWORDS),
                findFirst(headers, ISSUED_DATE_KEYWORDS),
                findFirst(headers, SUPPLIER_BIZ_NO_KEYWORDS),
                findFirst(headers, BUYER_BIZ_NO_KEYWORDS),
                findByDirection(headers, BUYER_NAME_KEYWORDS, preferSupplierSide),
                findFirst(headers, SUPPLY_AMOUNT_KEYWORDS),
                findFirst(headers, TAX_AMOUNT_KEYWORDS),
                findFirst(headers, TOTAL_AMOUNT_KEYWORDS),
                findFirst(headers, ITEM_NAME_KEYWORDS),
                findByDirection(headers, CEO_NAME_KEYWORDS, preferSupplierSide),
                findByDirection(headers, SUB_BIZ_NO_KEYWORDS, preferSupplierSide),
                findFirst(headers, MEMO_KEYWORDS)
        );

        return new TaxInvoiceCsvRecommendation(recommendedType, mapping);
    }

    /**
     * 사용자 정정(2026-08-13) — 처음엔 공급자/공급받는자 사업자번호 컬럼 중 어느 쪽에 값이 채워져 있는지로
     * 추천했는데(2026-08-12), 실제 세금계산서는 둘 다 항상 채워져 있어서 이 기준으로는 구분이 안 된다는
     * 걸 사용자가 확인함. 대신 헤더 위 제목 줄(예: "2022년도 매출세금계산서")에 "매출"/"매입" 키워드가
     * 있으면 그걸로 추천한다 — 어차피 비확정값이라 사용자가 라디오 버튼으로 최종 확정한다.
     */
    private String recommendType(String titleText) {
        if (titleText == null) {
            return null;
        }
        if (titleText.contains("매출")) {
            return "INCOME";
        }
        if (titleText.contains("매입")) {
            return "OUTCOME";
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

    /**
     * "상호"/"대표자명"/"종사업장번호"는 공급자·공급받는자 블록에 각각 있어서 헤더에 같은 이름이 두 번
     * 나온다(TaxInvoiceCsvParser/TaxInvoiceExcelParser가 두 번째부터 " (2)"를 붙여 구분해둔다). 세금계산서는
     * 항상 공급자 블록이 먼저, 공급받는자 블록이 나중이므로, 같은 키워드로 매치되는 후보 중
     * preferFirstOccurrence면 첫 번째(공급자 쪽, 접미사 없음)를, 아니면 마지막(공급받는자 쪽, 가장 큰
     * 접미사 번호)을 고른다. 후보가 1개뿐이면(중복 없는 파일) 둘 다 그 하나를 가리키므로 기존 동작과 같다.
     */
    private String findByDirection(List<String> headers, List<String> keywords, boolean preferFirstOccurrence) {
        for (String keyword : keywords) {
            List<String> matches = new java.util.ArrayList<>();
            for (String header : headers) {
                if (header.contains(keyword)) {
                    matches.add(header);
                }
            }
            if (!matches.isEmpty()) {
                return preferFirstOccurrence ? matches.get(0) : matches.get(matches.size() - 1);
            }
        }
        return null;
    }
}
