package com.group3.vitamins.settlement.application.usecase;

import com.group3.vitamins.settlement.application.command.UpdateSettlementItemCommand;
import com.group3.vitamins.settlement.domain.model.SettlementStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface SettlementCommandUseCase {

    //정산 항목 작성/수정
    UpdateSettlementItemView upsertItem(UpdateSettlementItemCommand command);

    //컨트롤러에 전달할 결과. accountNumber는 마스킹된 값만 담는다(요청 평문 기준 마스킹).
    record UpdateSettlementItemView(
            Long settleId,
            Integer roundNo,
            Long totalAmount,
            Long plannedAmount,
            Long plannedTaxAmount,
            LocalDate plannedDate,
            String traderName,
            String bankName,
            String maskedAccountNumber,
            String accountHolder,
            Long actualAmount,
            LocalDateTime actualDate,
            SettlementStatus status,
            Double paidAmountRatio,
            LocalDateTime createdAt,
            int version
    ) {
    }
}
