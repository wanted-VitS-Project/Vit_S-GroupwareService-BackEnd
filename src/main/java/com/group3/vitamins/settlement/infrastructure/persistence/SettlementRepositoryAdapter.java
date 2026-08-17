package com.group3.vitamins.settlement.infrastructure.persistence;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.settlement.domain.exception.SettlementErrorCode;
import com.group3.vitamins.settlement.domain.model.Settlement;
import com.group3.vitamins.settlement.domain.model.SettlementStatus;
import com.group3.vitamins.settlement.domain.model.SettlementType;
import com.group3.vitamins.settlement.domain.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class SettlementRepositoryAdapter implements SettlementRepository {

    private final SpringDataSettlementRepository springDataSettlementRepository;

    @Override
    @Transactional
    public Long create(Long blockId, Long projectId) {
        // IDENTITY 라 save() 시점에 INSERT 가 나가고 PK 가 채워져 돌아온다 — 되찾기 조회가 필요없다.
        return springDataSettlementRepository.save(new SettlementJpaEntity(blockId, projectId)).getSettleId();
    }

    @Override
    public Optional<Settlement> findActiveBySettleId(Long settleId) {
        return springDataSettlementRepository.findById(settleId)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public Settlement updateItem(Long settleId, SettlementType type, Integer roundNo, Long totalAmount,
                                  Long plannedAmount, Long plannedTaxAmount, LocalDate plannedDate,
                                  LocalDate taxInvoiceDueDate, String traderName, String bankName, String encryptedAccountNumber,
                                  String accountHolder, int expectedVersion) {
        // deleted_at IS NULL·status = PENDING·version = expectedVersion 조건을 UPDATE 문 자체에 걸어서
        // "확인 후 쓰기" 사이의 틈을 없앤다. 호출자(SettlementCommandService)가 이 메서드를 부르기 직전에
        // SettlementSiblingLookupPort.findCurrentStateForUpdate로 이 행을 FOR UPDATE 잠금 하에 다시 읽어
        // 삭제·연결 여부를 이미 확정했고, 그 잠금은 같은 트랜잭션이 끝날 때까지 유지된다 — 그래서 이 UPDATE가
        // 0건이면 deleted_at·status는 이미 검증된 값 그대로일 수밖에 없고, 남은 원인은 version 불일치뿐이다
        // (2026-08-12, CodeRabbit — 갱신 실패 후 일반 조회로 원인을 재분류하면 REPEATABLE READ 스냅샷 때문에
        // 부정확할 수 있다는 지적을 "쓰기 전에 잠금 하에 미리 확정"으로 근본 해결해서, 여기서는 재분류가
        // 필요 없어졌다).
        int updated = springDataSettlementRepository.updateItemIfActive(
                settleId, type, roundNo,
                toDecimal(totalAmount), toDecimal(plannedAmount), toDecimal(plannedTaxAmount),
                plannedDate, taxInvoiceDueDate, traderName, bankName, encryptedAccountNumber, accountHolder,
                SettlementStatus.PENDING, expectedVersion);
        if (updated == 0) {
            throw new ConflictException(SettlementErrorCode.SETTLEMENT_VERSION_CONFLICT);
        }

        SettlementJpaEntity entity = springDataSettlementRepository.findById(settleId)
                .orElseThrow(() -> new IllegalStateException("settlement not found after update: " + settleId));
        return toDomain(entity);
    }

    @Override
    @Transactional
    public boolean markDeleted(Long settleId, LocalDateTime deletedAt) {
        // 조건부 UPDATE: 이미 삭제된 행이면 0건 갱신 → 중복 이벤트로 간주하고 false 반환.
        int updated = springDataSettlementRepository.markDeletedIfActive(settleId, deletedAt);
        return updated > 0;
    }

    private Settlement toDomain(SettlementJpaEntity entity) {
        return Settlement.reconstruct(
                entity.getSettleId(),
                entity.getBlockId(),
                entity.getRoundNo(),
                entity.getType(),
                entity.getStatus(),
                toLong(entity.getTotalAmount()),
                toLong(entity.getPlannedAmount()),
                toLong(entity.getPlannedTaxAmount()),
                entity.getPlannedDate(),
                entity.getTaxInvoiceDueDate(),
                toLong(entity.getActualAmount()),
                entity.getActualDate(),
                entity.getTraderName(),
                entity.getBankName(),
                entity.getAccountNumber(),
                entity.getAccountHolder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt(),
                entity.getVersion()
        );
    }

    private BigDecimal toDecimal(Long value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private Long toLong(BigDecimal value) {
        return value == null ? null : value.longValue();
    }
}
