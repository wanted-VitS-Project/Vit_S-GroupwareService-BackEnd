package com.group3.vitamins.settlement.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

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
        String accountHolder,

        @NotNull(message = "SETTLEMENT_VERSION_REQUIRED|버전 정보가 없습니다. 화면을 새로고침해 주세요.")
        @Schema(description = "블록 목록 조회에서 받은 version 을 그대로 실어 보낸다. "
                + "그 사이 남이 먼저 저장했으면 409 다", example = "1")
        Integer version,

        @Schema(description = "true 면 충돌을 무시하고 덮어쓴다. 생략하면 false", example = "false")
        Boolean overwrite
) {
}
