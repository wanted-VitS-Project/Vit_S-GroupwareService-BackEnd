package com.group3.vitamins.settlement.infrastructure.blockdetail;

import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.application.result.BlockDetail;
import com.group3.vitamins.project.block.application.result.SettlementDetail;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.settlement.application.service.SettlementHandlerService;
import com.group3.vitamins.settlement.domain.model.SettlementProgress;
import com.group3.vitamins.settlement.infrastructure.security.AccountNumberCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SettlementBlockDetailAdapter implements BlockDetailPort {

    private final SettlementDetailMapper settlementDetailMapper;
    private final SettlementHandlerService settlementHandlerService;
    private final AccountNumberCipher accountNumberCipher;

    @Override
    public BlockType supportedType() {
        return BlockType.SETTLEMENT;
    }

    /** 내용이 빈 행을 만든다. INSERT 는 정산 도메인이 JPA 로 처리하고 PK 를 돌려준다. */
    @Override
    public Long createDetail(Long blockId) {
        return settlementHandlerService.create(blockId);
    }

    /** 정산 도메인의 삭제 처리를 재사용한다 — 멱등 판정이 그쪽에 있다. */
    @Override
    public void deleteDetail(Long typeId, String userId, String blockTitle, LocalDateTime deletedAt) {
        settlementHandlerService.delete(typeId, userId, blockTitle, deletedAt);
    }

    @Override
    public Map<Long, BlockDetail> loadDetails(Collection<Long> typeIds) {
        if (typeIds.isEmpty()) {
            return Map.of();
        }
        return settlementDetailMapper.findBySettleIds(typeIds).stream()
                .collect(Collectors.toMap(SettlementDetailRow::settleId, this::toDetail));
    }

    private SettlementDetail toDetail(SettlementDetailRow row) {
        return new SettlementDetail(
                row.settleId(),
                row.roundNo(),
                row.type(),
                row.status(),
                row.totalAmount(),
                row.plannedAmount(),
                row.plannedTaxAmount(),
                row.plannedDate(),
                row.traderName(),
                row.bankName(),
                accountNumberCipher.decryptAndMask(row.accountNumber()),
                row.accountHolder(),
                row.actualAmount(),
                row.actualDate(),
                SettlementProgress.ratio(row.actualAmountSum(), row.totalAmount()),
                row.createdAt(),
                row.version()
        );
    }
}
