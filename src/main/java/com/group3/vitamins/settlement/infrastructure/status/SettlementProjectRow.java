package com.group3.vitamins.settlement.infrastructure.status;

import java.time.LocalDate;

/**
 * 정산 현황 프로젝트 조회용 프로젝트 단위 집계 행.
 *
 * @param totalPlannedAmount 이 프로젝트 INCOME 정산 블록들의 total_amount(SETL-008로 회차 간 통일된 값).
 *                            INCOME 정산 블록이 하나도 없으면 null
 * @param totalOutcome OUTCOME 정산 블록 전체의 actual_amount 합계 (없으면 0)
 * @param totalIncome INCOME 정산 블록 전체의 actual_amount 합계 (없으면 0)
 * @param completedRoundCount status=COMPLETED 인 활성 정산 블록 개수 (INCOME+OUTCOME 합산)
 * @param totalRoundCount 활성 정산 블록 전체 개수 (INCOME+OUTCOME 합산)
 * @param pendingRoundCount 입출금 미연결(status IN ('PENDING','WAITING')) 인 활성 정산 블록 개수 —
 *                          statusSummary 계산용, 응답 필드 아님. WAITING(세금계산서만 붙음)도 포함한다(2026-08-13)
 * @param paymentOverdueDays 입출금 기한이 지났는데 아직 미연결인 회차의 최대 경과일 (지연 없으면 0)
 * @param taxInvoiceOverdueDays 세금계산서 기한이 지났는데 아직 미연결인 회차의 최대 경과일 (지연 없으면 0)
 * @param taxInvoiceUnlinkedCount 세금계산서가 연결되지 않은 활성 정산 블록 개수 — status로는 판정할 수 없어
 *                                tax_invoice를 직접 확인한다(WAITING은 세금계산서가 붙은 상태라 제외된다)
 * @param nextPlannedDate 미완료(status != COMPLETED) 정산 블록 중 round_no 가 가장 낮은 것의 planned_date
 */
public record SettlementProjectRow(
        Long projectId,
        String projectName,
        String clientName,
        String projectManager,
        Long totalPlannedAmount,
        Long totalOutcome,
        Long totalIncome,
        Long completedRoundCount,
        Long totalRoundCount,
        Long pendingRoundCount,
        Long taxInvoiceUnlinkedCount,
        Long paymentOverdueDays,
        Long taxInvoiceOverdueDays,
        LocalDate nextPlannedDate,
        String projectStatus,
        LocalDate endedOn
) {
}
