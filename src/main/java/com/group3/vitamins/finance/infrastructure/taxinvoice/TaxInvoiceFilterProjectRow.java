package com.group3.vitamins.finance.infrastructure.taxinvoice;

/** 필터 옵션용 — tax_invoice가 하나라도 연결된 정산 블록을 가진 프로젝트만. */
public record TaxInvoiceFilterProjectRow(Long projectId, String projectName) {
}
