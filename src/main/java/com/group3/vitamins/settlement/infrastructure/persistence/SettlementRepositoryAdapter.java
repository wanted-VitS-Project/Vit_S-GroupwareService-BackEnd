package com.group3.vitamins.settlement.infrastructure.persistence;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
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
    public Long create(Long blockId) {
        // IDENTITY 라 save() 시점에 INSERT 가 나가고 PK 가 채워져 돌아온다 — 되찾기 조회가 필요없다.
        return springDataSettlementRepository.save(new SettlementJpaEntity(blockId)).getSettleId();
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
                                  String traderName, String bankName, String encryptedAccountNumber,
                                  String accountHolder, int expectedVersion) {
        // deleted_at IS NULL·status = PENDING·version = expectedVersion 조건을 UPDATE 문 자체에 걸어서
        // "확인 후 쓰기" 사이의 틈을 없앤다 — 이 메서드를 호출하기 전에 이미 assertModifiable로 PENDING임을
        // 확인했지만, 그건 락 걸기 전에 읽은 값이라 그 사이 다른 트랜잭션이 연결(WAITING 등)했거나 먼저
        // 저장(version 변경)했을 수 있다. 그 경우도 여기서 막는다(CONCURRENCY.md §3).
        int updated = springDataSettlementRepository.updateItemIfActive(
                settleId, type, roundNo,
                toDecimal(totalAmount), toDecimal(plannedAmount), toDecimal(plannedTaxAmount),
                plannedDate, traderName, bankName, encryptedAccountNumber, accountHolder,
                SettlementStatus.PENDING, expectedVersion);
        if (updated == 0) {
            // 0건이 된 원인을 삭제/상태변경(연결)/버전충돌 셋으로 구분한다 — 존재하고 삭제도 안 됐고
            // 상태도 여전히 PENDING인데 갱신이 안 됐다면 그 사이 남이 먼저 저장해 version이 어긋난
            // 것이다(낙관락 충돌, CONCURRENCY.md §3).
            SettlementJpaEntity current = springDataSettlementRepository.findById(settleId).orElse(null);
            if (current == null || current.getDeletedAt() != null) {
                throw new NotFoundException(SettlementErrorCode.BLOCK_NOT_FOUND);
            }
            if (current.getStatus() != SettlementStatus.PENDING) {
                throw new ConflictException(SettlementErrorCode.ALREADY_LINKED);
            }
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
