package com.group3.vitamins.finance.infrastructure.taxinvoice;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 매칭 추천 조회의 기준이 되는 세금계산서 원본 값(type/totalAmount/taxAmount/issuedNo/buyerName)만 뽑은 행. */
public record TaxInvoiceBasicRow(
        String type,
        BigDecimal totalAmount,
        BigDecimal taxAmount,
        LocalDate issuedNo,
        String buyerName
) {
}
