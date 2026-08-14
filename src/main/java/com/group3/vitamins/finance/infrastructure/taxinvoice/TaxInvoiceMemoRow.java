package com.group3.vitamins.finance.infrastructure.taxinvoice;

import java.time.LocalDateTime;

/** 메모 수정 응답 조립용 — 수정 직후 다시 조회한 값. */
public record TaxInvoiceMemoRow(
        Long taxId,
        String memo,
        LocalDateTime updatedAt
) {
}
