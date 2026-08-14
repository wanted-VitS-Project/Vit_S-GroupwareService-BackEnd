package com.group3.vitamins.finance.infrastructure.taxinvoice;

import java.time.LocalDateTime;

/** 매칭 완료 후 응답 조립용 — 매칭 UPDATE가 커밋되기 전, 같은 트랜잭션 안에서 다시 조회한다. */
public record TaxInvoiceMatchResultRow(
        Long settleId,
        String roundName,
        String projectName,
        String linkedBy,
        String linkedByName,
        LocalDateTime linkedAt
) {
}
