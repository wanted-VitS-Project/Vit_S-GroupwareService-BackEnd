package com.group3.vitamins.finance.application.command;

/**
 * 세금계산서는 수동 등록이 없어(전부 CSV/엑셀 업로드로 들어온다) 수정 대상이 메모뿐이다 —
 * 입출금 수정 API가 {@code sourceType != MANUAL} 이면 메모만 허용하는 것과 같은 이유다.
 */
public record UpdateTaxInvoiceMemoCommand(
        Long taxId,
        String memo,
        String userId,
        String role
) {
}
