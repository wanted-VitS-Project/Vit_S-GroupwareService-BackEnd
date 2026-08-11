package com.group3.vitamins.finance.infrastructure.cashflow;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 매칭 추천 조회의 기준이 되는 입출금 내역 원본 값(type/amount/tradedAt/depositorName)만 뽑은 행. */
public record CashFlowBasicRow(
        String type,
        BigDecimal amount,
        LocalDateTime tradedAt,
        String depositorName
) {
}
