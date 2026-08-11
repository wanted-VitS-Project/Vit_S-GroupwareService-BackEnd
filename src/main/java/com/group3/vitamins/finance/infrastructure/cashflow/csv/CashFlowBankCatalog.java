package com.group3.vitamins.finance.infrastructure.cashflow.csv;

import java.util.List;

/**
 * 은행명 드롭다운 옵션 — 아직 은행 카탈로그 테이블이 없어서(사용자 확정, 2026-08-10) 명세의
 * Success Example에 있던 8개를 그대로 임시 하드코딩한다. 나중에 테이블이 생기면 이 상수만 걷어내면 된다.
 */
public final class CashFlowBankCatalog {

    public static final List<String> BANK_OPTIONS = List.of(
            "신한은행", "국민은행", "카카오뱅크", "우리은행", "농협은행", "하나은행", "기업은행", "새마을금고"
    );

    private CashFlowBankCatalog() {
    }
}
