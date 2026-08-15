package com.group3.vitamins.finance.infrastructure.taxinvoice.csv;

import java.math.BigDecimal;
import java.time.LocalDate;

/** type은 담지 않는다 — 업로드 요청 전체에 하나뿐인 값이라(라디오 버튼) 행마다 반복해서 들고 있을 필요가 없다. */
public record ParsedTaxInvoiceRow(
        String approvalNo,
        LocalDate issuedNo,
        String supplierBizNo,
        String buyerBizNo,
        String buyerName,
        BigDecimal supplyAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String itemName,
        String ceoName,
        String subBizNo,
        String memo
) {
}
