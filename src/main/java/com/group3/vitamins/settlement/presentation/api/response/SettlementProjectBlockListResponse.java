package com.group3.vitamins.settlement.presentation.api.response;

import com.group3.vitamins.settlement.application.usecase.SettlementQueryUseCase.SettlementProjectBlockListView;
import com.group3.vitamins.settlement.application.usecase.SettlementQueryUseCase.SettlementProjectBlockView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SettlementProjectBlockListResponse(
        @Schema(description = "회차별 정산 블록 목록")
        List<SettlementBlockItem> blocks
) {

    public static SettlementProjectBlockListResponse from(SettlementProjectBlockListView view) {
        return new SettlementProjectBlockListResponse(view.blocks().stream().map(SettlementBlockItem::from).toList());
    }

    public record SettlementBlockItem(
            @Schema(description = "정산 블록 아이디", example = "10")
            Long settleId,

            @Schema(description = "회차 번호", example = "1", nullable = true)
            Integer roundNo,

            @Schema(description = "회차명(정산 블록명)", example = "1차 정산(선급 60%)", nullable = true)
            String roundName,

            @Schema(description = "예정일", example = "2026-05-10", nullable = true)
            LocalDate plannedDate,



            @Schema(description = "예정금액", example = "270000000", nullable = true)
            Long plannedAmount,

            @Schema(description = "예정 세금 금액", example = "27000000", nullable = true)
            Long plannedTaxAmount,

            @Schema(description = "세금계산서 발행일. 연결된 세금계산서가 없으면 null", example = "2026-05-15", nullable = true)
            LocalDate taxInvoiceDate,

            @Schema(description = "세금계산서 금액 — 이 정산 블록의 예정 세금 금액(plannedTaxAmount)과 같은 값이다",
                    example = "27000000", nullable = true)
            Long taxInvoiceAmount,

            @Schema(description = "입출금 구분", example = "INCOME", nullable = true)
            String paidType,

            @Schema(description = "은행명. OUTCOME인 경우만 값 있음", example = "신한은행", nullable = true)
            String bankName,

            @Schema(description = "계좌번호(마스킹 없는 원본). OUTCOME인 경우만 값 있음 — 이 API는 재무팀만 접근 가능"
                    + "(FINANCE 페이지 권한)", example = "100555074444", nullable = true)
            String accountNumber,

            @Schema(description = "예금주. OUTCOME인 경우만 값 있음", example = "홍길동", nullable = true)
            String accountHolder,

            @Schema(description = "실제 입출금일", example = "2026-06-30", nullable = true)
            LocalDate paidDate,

            @Schema(description = "실제 입출금액", example = "270000000", nullable = true)
            Long paidAmount,

            @Schema(description = "정산 블록 회차 상태", example = "COMPLETED")
            String status,

            @Schema(description = "세금계산서 매칭 처리자 사번. 연결된 세금계산서가 없으면 null", example = "EMP010", nullable = true)
            String taxLinkedBy,

            @Schema(description = "세금계산서 매칭 처리자 이름", example = "이과장", nullable = true)
            String taxLinkedByName,

            @Schema(description = "세금계산서 매칭 처리일시", example = "2026-05-15T10:00:00", nullable = true)
            LocalDateTime taxLinkedAt,

            @Schema(description = "입출금 매칭 처리자 사번. 연결된 입출금 내역이 없으면 null", example = "EMP010", nullable = true)
            String cashFlowLinkedBy,

            @Schema(description = "입출금 매칭 처리자 이름", example = "이과장", nullable = true)
            String cashFlowLinkedByName,

            @Schema(description = "입출금 매칭 처리일시", example = "2026-06-30T14:00:00", nullable = true)
            LocalDateTime cashFlowLinkedAt
    ) {

        public static SettlementBlockItem from(SettlementProjectBlockView view) {
            return new SettlementBlockItem(
                    view.settleId(),
                    view.roundNo(),
                    view.roundName(),
                    view.plannedDate(),
                    view.plannedAmount(),
                    view.plannedTaxAmount(),
                    view.taxInvoiceDate(),
                    view.taxInvoiceAmount(),
                    view.paidType(),
                    view.bankName(),
                    view.accountNumber(),
                    view.accountHolder(),
                    view.paidDate(),
                    view.paidAmount(),
                    view.status(),
                    view.taxLinkedBy(),
                    view.taxLinkedByName(),
                    view.taxLinkedAt(),
                    view.cashFlowLinkedBy(),
                    view.cashFlowLinkedByName(),
                    view.cashFlowLinkedAt()
            );
        }
    }
}
