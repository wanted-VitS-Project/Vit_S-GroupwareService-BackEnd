package com.group3.vitamins.finance.infrastructure.taxinvoice.csv;

/** 세금계산서 CSV 업로드용 컬럼 매핑. itemName/ceoName/subBizNo/memo 넷만 선택, 나머지는 필수. */
public record TaxInvoiceCsvMapping(
        String approvalNoColumn,
        String issuedDateColumn,
        String supplierBizNoColumn,
        String buyerBizNoColumn,
        String buyerNameColumn,
        String supplyAmountColumn,
        String taxAmountColumn,
        String totalAmountColumn,
        String itemNameColumn,
        String ceoNameColumn,
        String subBizNoColumn,
        String memoColumn
) {
}
