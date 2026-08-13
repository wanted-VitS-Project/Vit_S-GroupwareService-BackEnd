package com.group3.vitamins.finance.infrastructure.cashflow.csv;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CSV 헤더명을 키워드로 매칭해 컬럼 매핑을 추천한다. ⚠️ 명세에 정확한 판정 규칙이 없어 헤더명 키워드
 * 사전 기반으로 직접 설계했다 — 실제 은행 CSV 샘플로 검증 전까지는 최선 추정치다. 추천이 틀려도 사용자가
 * 화면에서 드롭다운으로 직접 고쳐 확정(업로드 API의 request)하므로 최종 저장 값에는 영향 없다.
 */
@Component
public class CashFlowCsvColumnRecommender {

    private static final List<String> DATETIME_KEYWORDS = List.of("일시");
    private static final List<String> DATE_KEYWORDS = List.of("일자", "날짜");
    private static final List<String> TIME_KEYWORDS = List.of("시간", "시각");
    private static final List<String> INCOME_KEYWORDS = List.of("입금액", "입금");
    private static final List<String> OUTCOME_KEYWORDS = List.of("출금액", "출금");
    private static final List<String> AMOUNT_KEYWORDS = List.of("거래금액", "금액");
    private static final List<String> TYPE_KEYWORDS = List.of("입출금구분", "거래구분", "구분");
    // ⚠️ "내용"은 여기 넣지 않는다 (2026-08-13 정정) — DEPOSITOR_KEYWORDS에도 있어서, "적요"가 없고
    // "내용"만 있는 파일(카카오뱅크 등)이면 memoColumn·depositorColumn이 같은 컬럼으로 추천됐다.
    // 그러면 bank_memo와 depositor_name에 똑같은 문자열이 중복 저장된다. depositorName은 필수(NOT NULL)고
    // 메모는 선택이라, 후보가 하나뿐이면 필수인 쪽에 주고 메모는 비워두는 게 맞다.
    // (프론트에서 사용자가 메모를 같은 컬럼으로 직접 지정하는 건 여전히 가능하다 — 추천값만 바뀐 것이다.)
    // "비고"는 실제 은행 파일에서 쓰이는데 빠져 있어 같이 추가했다(세금계산서 쪽 목록엔 이미 있었다).
    private static final List<String> MEMO_KEYWORDS = List.of("적요", "메모", "비고");
    // "내용"은 맨 뒤 — 은행 CSV에서 거래처(예금주) 정보가 별도 컬럼 없이 "내용" 컬럼에 실려오는 경우가
    // 흔하다(2026-08-10, 실제 파일로 확인). "거래처"/"입금자" 등 더 명확한 컬럼이 있으면 그게 우선이고,
    // 없을 때만 "내용"을 대체 후보로 추천한다.
    private static final List<String> DEPOSITOR_KEYWORDS = List.of("입금자", "송금인", "거래처", "이름", "내용");
    private static final List<String> BALANCE_KEYWORDS = List.of("잔액");

    public CashFlowCsvRecommendation recommend(List<String> headers) {
        String dateTimeColumn = findFirst(headers, DATETIME_KEYWORDS);
        String dateColumn = findFirst(headers, DATE_KEYWORDS);
        String timeColumn = findFirst(headers, TIME_KEYWORDS);

        CashFlowDateTimeMode dateTimeMode;
        String recommendedDateTimeColumn = null;
        String recommendedDateColumn = null;
        String recommendedTimeColumn = null;
        if (dateColumn != null && timeColumn != null) {
            dateTimeMode = CashFlowDateTimeMode.SEPARATE;
            recommendedDateColumn = dateColumn;
            recommendedTimeColumn = timeColumn;
        } else {
            // "일시" 컬럼이 있으면 그걸, 없으면 "일자/날짜" 컬럼을(시간 없이 자정으로 간주) 통합 컬럼으로 본다.
            dateTimeMode = CashFlowDateTimeMode.SINGLE;
            recommendedDateTimeColumn = dateTimeColumn != null ? dateTimeColumn : dateColumn;
        }

        String incomeColumn = findFirst(headers, INCOME_KEYWORDS);
        String outcomeColumn = findFirst(headers, OUTCOME_KEYWORDS);
        String amountColumn = findFirst(headers, AMOUNT_KEYWORDS);
        String typeColumn = findTypeColumn(headers);

        CashFlowAmountMode amountMode;
        String recommendedIncomeColumn = null;
        String recommendedOutcomeColumn = null;
        String recommendedAmountColumn = null;
        String recommendedTypeColumn = null;
        if (incomeColumn != null && outcomeColumn != null) {
            amountMode = CashFlowAmountMode.SEPARATE;
            recommendedIncomeColumn = incomeColumn;
            recommendedOutcomeColumn = outcomeColumn;
        } else {
            amountMode = CashFlowAmountMode.SINGLE_WITH_TYPE;
            recommendedAmountColumn = amountColumn;
            recommendedTypeColumn = typeColumn;
        }

        String memoColumn = findFirst(headers, MEMO_KEYWORDS);
        String depositorColumn = findFirst(headers, DEPOSITOR_KEYWORDS);
        String balanceColumn = findFirst(headers, BALANCE_KEYWORDS);

        CashFlowCsvMapping mapping = new CashFlowCsvMapping(
                recommendedDateTimeColumn, recommendedDateColumn, recommendedTimeColumn,
                recommendedAmountColumn, recommendedTypeColumn,
                recommendedIncomeColumn, recommendedOutcomeColumn,
                memoColumn, depositorColumn, balanceColumn
        );

        return new CashFlowCsvRecommendation(dateTimeMode, amountMode, mapping);
    }

    /**
     * "구분"과 정확히 이름이 같은 컬럼이 있으면 최우선으로 쓴다 — 실제 은행 CSV에 "구분"(입금/출금이
     * 그대로 적힘)과 "거래구분"(계좌간자동이체 등 거래 방식, 방향과 무관)이 같이 있는 경우가 있어서
     * (2026-08-10, 실제 파일로 확인), 부분 일치만으로는 "거래구분"이 먼저 걸려 잘못 추천됐었다.
     * 정확히 "구분"인 컬럼이 없을 때만 기존 부분 일치 목록(TYPE_KEYWORDS)으로 대체한다.
     */
    private String findTypeColumn(List<String> headers) {
        for (String header : headers) {
            if ("구분".equals(header)) {
                return header;
            }
        }
        return findFirst(headers, TYPE_KEYWORDS);
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
