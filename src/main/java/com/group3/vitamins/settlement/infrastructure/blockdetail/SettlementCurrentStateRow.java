package com.group3.vitamins.settlement.infrastructure.blockdetail;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * lockSiblingSettlementBlocksForUpdate 직후, FOR UPDATE로 다시 읽은 이 settleId 행의 현재 상태 전체.
 * 삭제·연결 판정용 3필드(version/status/deletedAt) + 타입 다운그레이드 판정·활동 로그 이전값 비교용
 * 필드(나머지)를 한 번의 조회로 같이 담는다.
 */
public record SettlementCurrentStateRow(
        Integer version, String status, LocalDateTime deletedAt,
        String type, Integer roundNo, Long totalAmount, Long plannedAmount, Long plannedTaxAmount,
        LocalDate plannedDate,
        LocalDate taxInvoiceDueDate, String traderName, String bankName, String accountNumber, String accountHolder
) {
}
