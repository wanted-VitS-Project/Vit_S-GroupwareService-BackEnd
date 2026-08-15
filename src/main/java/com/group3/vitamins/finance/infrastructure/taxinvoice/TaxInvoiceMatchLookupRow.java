package com.group3.vitamins.finance.infrastructure.taxinvoice;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 매칭/매칭 해제 검증에 필요한 세금계산서 원본 값. {@code settleBlockId}는 현재 연결 여부 판정용,
 * {@code isExcluded}는 제외 처리된 항목의 매칭을 막기 위한 것이다(2026-08-13 추가).
 */
public record TaxInvoiceMatchLookupRow(
        String type,
        BigDecimal totalAmount,
        LocalDate issuedNo,
        Long settleBlockId,
        Boolean isExcluded
) {
}
