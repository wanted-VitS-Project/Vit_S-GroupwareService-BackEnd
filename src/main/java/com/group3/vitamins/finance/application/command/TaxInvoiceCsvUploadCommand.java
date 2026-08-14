package com.group3.vitamins.finance.application.command;

public record TaxInvoiceCsvUploadCommand(
        byte[] fileBytes,
        String fileName,
        String password,
        String type,
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
        String memoColumn,
        String userId,
        String role
) {
}
