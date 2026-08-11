package com.group3.vitamins.finance.infrastructure.cashflow;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 매칭/매칭 해제 검증에 필요한 입출금 내역 원본 값. {@code settleBlockId}는 현재 연결 여부 판정용. */
public record CashFlowMatchLookupRow(
        String type,
        BigDecimal amount,
        LocalDateTime tradedAt,
        Long settleBlockId
) {
}
