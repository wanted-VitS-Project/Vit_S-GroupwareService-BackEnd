package com.group3.vitamins.settlement.presentation.api.response;

import com.group3.vitamins.settlement.application.usecase.SettlementCommandUseCase.UpdateSettlementItemView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SettlementItemResponse(
        @Schema(description = "정산 블록 ID", example = "1")
        Long settleId,

        @Schema(description = "정산 회차", example = "1")
        Integer roundNo,

        @Schema(description = "프로젝트 정산 예정 총 금액", example = "4500000")
        Long totalAmount,

        @Schema(description = "회차별 정산 예정 금액", example = "1500000")
        Long plannedAmount,

        @Schema(description = "회차별 정산 예정 세금 금액", example = "200000")
        Long plannedTaxAmount,

        @Schema(description = "회차별 정산 예정일", example = "2026-09-01")
        LocalDate plannedDate,

        @Schema(description = "거래처명(입금자명)", example = "(주)대한항공")
        String traderName,

        @Schema(description = "은행명. type=OUTCOME 인 경우만 값이 있다", example = "신한은행", nullable = true)
        String bankName,

        @Schema(description = "계좌번호. 앞·뒤 3자리만 남기고 마스킹. type=OUTCOME 인 경우만 값이 있다",
                example = "100******444", nullable = true)
        String accountNumber,

        @Schema(description = "예금주. type=OUTCOME 인 경우만 값이 있다", example = "홍길동", nullable = true)
        String accountHolder,

        @Schema(description = "재무팀에서 입력할 실제 입출금 금액. 미확정이면 null", nullable = true)
        Long actualAmount,

        @Schema(description = "재무팀에서 입력할 실제 입출금 시간. 미확정이면 null", nullable = true)
        LocalDateTime actualDate,

        @Schema(description = "정산 상태", example = "PENDING")
        String status,

        @Schema(description = "금액 기준 진행률 — 이 블록 하나가 아니라 같은 프로젝트·같은 타입(INCOME/OUTCOME) "
                + "정산 블록 전체의 실제 금액 합계를 이 타입의 프로젝트 총 예정 금액(totalAmount)으로 나눈 값",
                example = "0.0")
        Double paidAmountRatio,

        @Schema(description = "정산 블록에 내용이 생성된 일시")
        LocalDateTime createdAt,

        @Schema(description = "수정 후 버전 — 다음 수정 요청에 그대로 실어 보낸다", example = "2")
        int version
) {

    public static SettlementItemResponse from(UpdateSettlementItemView view) {
        return new SettlementItemResponse(
                view.settleId(),
                view.roundNo(),
                view.totalAmount(),
                view.plannedAmount(),
                view.plannedTaxAmount(),
                view.plannedDate(),
                view.traderName(),
                view.bankName(),
                view.maskedAccountNumber(),
                view.accountHolder(),
                view.actualAmount(),
                view.actualDate(),
                view.status().name(),
                view.paidAmountRatio(),
                view.createdAt(),
                view.version()
        );
    }
}
