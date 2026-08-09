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
    public SiblingRecommendation findSiblingRecommendation(Long settleId, SettlementType type) {
        SettlementRecommendationRow row = settlementSiblingMapper.findRecommendation(settleId, type.name());
        return row == null ? null : new SiblingRecommendation(row.blockCount(), row.recommendedTotalAmount());
    }
}
