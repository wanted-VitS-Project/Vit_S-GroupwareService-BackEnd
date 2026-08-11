package com.group3.vitamins.finance.infrastructure.cashflow.csv;

/** CSV 헤더명 → 우리 필드 매핑. 각 필드는 모드에 따라 없을 수 있어(null) 전부 nullable이다. */
public record CashFlowCsvMapping(
        String tradedDateTimeColumn,
        String tradedDateColumn,
        String tradedTimeColumn,
        String amountColumn,
        String typeColumn,
        String incomeAmountColumn,
        String outcomeAmountColumn,
        String memoColumn,
        String depositorColumn,
        // 원 명세의 추천 매핑 9종엔 없던 필드(2026-08-10 추가) — 같은 은행·시각·금액인데 실제로는
        // 다른 거래인 경우를 구분하는 중복 판정 보강용. CSV에 "잔액" 컬럼이 없으면 매핑 안 해도 된다(선택).
        String balanceColumn
) {
    public static final CashFlowCsvMapping EMPTY =
            new CashFlowCsvMapping(null, null, null, null, null, null, null, null, null, null);
}
