package com.group3.vitamins.finance.infrastructure.taxinvoice;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 매칭/매칭 해제 검증에 필요한 세금계산서 원본 값. {@code settleBlockId}는 현재 연결 여부 판정용. */
public record TaxInvoiceMatchLookupRow(
        String type,
        BigDecimal totalAmount,
        LocalDate issuedNo,
        Long settleBlockId
) {
}
