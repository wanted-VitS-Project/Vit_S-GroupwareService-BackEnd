package com.group3.vitamins.finance.infrastructure.cashflow;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 입출금 내역 등록/수정 응답 조립 + 수정 시 병합(merge)에 쓰는 상세 행.
 * {@code settleBlockId}는 응답엔 안 나가지만 "매칭됐으면 메모만 수정 가능" 판정에 필요하다.
 */
public record CashFlowDetailRow(
        Long cashFlowId,
        String bankTxnId,
        String bankName,
        LocalDateTime tradedAt,
        String type,
        BigDecimal amount,
        String depositorName,
        String memo,
        String sourceType,
        Long settleBlockId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
