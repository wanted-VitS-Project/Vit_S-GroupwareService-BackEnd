package com.group3.vitamins.settlement.infrastructure.blockdetail;

import com.group3.vitamins.settlement.application.port.SettlementSiblingLookupPort;
import com.group3.vitamins.settlement.domain.model.SettlementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementSiblingLookupAdapter implements SettlementSiblingLookupPort {

    private final SettlementSiblingMapper settlementSiblingMapper;

    @Override
    public Long findActualAmountSum(Long settleId, SettlementType type) {
        return settlementSiblingMapper.findActualAmountSum(settleId, type.name());
    }

    @Override
    public Long findEstablishedTotalAmount(Long settleId, SettlementType type) {
        return settlementSiblingMapper.findEstablishedTotalAmount(settleId, type.name());
    }

    @Override
    public void lockSiblingSettlementBlocksForUpdate(Long settleId) {
        settlementSiblingMapper.lockSiblingSettlementBlocksForUpdate(settleId);
    }

    @Override
    public SettlementCurrentState findCurrentStateForUpdate(Long settleId) {
        SettlementCurrentStateRow row = settlementSiblingMapper.findCurrentStateForUpdate(settleId);
        if (row == null) {
            return null;
        }
        return new SettlementCurrentState(
                row.version(), row.status(), row.deletedAt(),
                row.type(), row.roundNo(), row.totalAmount(), row.plannedAmount(), row.plannedTaxAmount(),
                row.plannedDate(), row.traderName(), row.bankName(), row.accountNumber(), row.accountHolder()
        );
    }

    @Override
    public SiblingRecommendation findSiblingRecommendation(Long settleId, SettlementType type) {
        SettlementRecommendationRow row = settlementSiblingMapper.findRecommendation(settleId, type.name());
        return row == null ? null : new SiblingRecommendation(row.maxRoundNo(), row.recommendedTotalAmount());
    }
}
