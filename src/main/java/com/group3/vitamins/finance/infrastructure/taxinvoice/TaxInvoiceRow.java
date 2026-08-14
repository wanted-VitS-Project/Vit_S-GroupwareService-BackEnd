package com.group3.vitamins.finance.infrastructure.taxinvoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 세금계산서 조회 — cash_flow의 findCashFlows/CashFlowRow와 동일한 사고방식(§linkStatus 포함). */
public record TaxInvoiceRow(
        Long taxId,
        LocalDate issuedNo,
        String approvalNo,
        String type,
        String buyerName,
        String buyerBizNo,
        String supplierBizNo,
        String subBizNo,
        String ceoName,
        String itemName,
        BigDecimal supplyAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String memo,
        String sourceType,
        Long projectId,
        String projectName,
        Long settleId,
        String roundName,
        String linkedBy,
        String linkedByName,
        LocalDateTime linkedAt,
        boolean isExcluded,
        String linkStatus
) {
}
