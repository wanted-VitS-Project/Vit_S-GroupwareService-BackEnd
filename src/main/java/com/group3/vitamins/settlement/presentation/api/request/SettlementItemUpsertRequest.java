package com.group3.vitamins.settlement.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record SettlementItemUpsertRequest(
        @Schema(description = "정산 회차", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer roundNo,

        @Schema(description = "프로젝트 정산 예정 총 금액", example = "4500000", requiredMode = Schema.RequiredMode.REQUIRED)
        Long totalAmount,

        @Schema(description = "회차별 정산 예정 금액", example = "1500000", requiredMode = Schema.RequiredMode.REQUIRED)
        Long plannedAmount,

        @Schema(description = "회차별 정산 예정 세금 금액", example = "200000", requiredMode = Schema.RequiredMode.REQUIRED)
        Long plannedTaxAmount,

        @Schema(description = "회차별 정산 예정일", example = "2026-09-01", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate plannedDate,

        @Schema(description = "거래처명(입금자명)", example = "(주)대한항공", requiredMode = Schema.RequiredMode.REQUIRED)
        String traderName,

        @Schema(description = "은행명. type=OUTCOME 인 경우만 필수", example = "신한은행", nullable = true)
        String bankName,

        @Schema(description = "계좌번호(하이픈·띄어쓰기 없이). type=OUTCOME 인 경우만 필수",
                example = "100555074444", nullable = true)
        String accountNumber,

        @Schema(description = "예금주. type=OUTCOME 인 경우만 필수", example = "홍길동", nullable = true)
        String accountHolder
) {
}
