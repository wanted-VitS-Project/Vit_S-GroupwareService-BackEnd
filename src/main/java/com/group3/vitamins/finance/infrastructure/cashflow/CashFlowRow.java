package com.group3.vitamins.finance.infrastructure.cashflow;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 입출금 내역 조회 한 행. {@code projectName}/{@code roundName}은 미연결이면 null이 그대로 내려온다.
 * {@code roundName}/{@code settleId}는 연결됐던 정산 블록이 삭제된 경우에도 계속 채워진다 —
 * {@code linkStatus}(UNLINKED/LINKED/LINK_BLOCK_DELETED)로 미연결과 "연결됐던 블록 삭제됨"을
 * 구분한다(2026-08-10).
 */
public record CashFlowRow(
        Long cashFlowId,
        LocalDateTime tradedAt,
        String bankTxnId,
        String type,
        BigDecimal amount,
        String depositorName,
        String bankMemo,
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
