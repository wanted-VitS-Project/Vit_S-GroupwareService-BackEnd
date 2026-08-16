package com.group3.vitamins.settlement.infrastructure.blockdetail;

import com.group3.vitamins.settlement.application.port.SettlementProjectLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementProjectLookupAdapter implements SettlementProjectLookupPort {

    private final SettlementProjectLookupMapper settlementProjectLookupMapper;

    @Override
    public Long findProjectIdByBlockId(Long blockId) {
        return settlementProjectLookupMapper.findProjectIdByBlockId(blockId);
    }
}
