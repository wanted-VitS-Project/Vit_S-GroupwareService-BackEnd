package com.group3.vitamins.settlement.domain.repository;

import com.group3.vitamins.settlement.domain.model.Settlement;
import com.group3.vitamins.settlement.domain.model.SettlementType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 정산 도메인이 바라보는 영속성 포트. 구현체는 infrastructure/persistence 에 있다.
 */
public interface SettlementRepository {

    /**
     * 내용이 빈 상세 행을 만들고 그 PK 를 돌려준다. 블록 생성 트랜잭션에서 Block 도메인이 호출한다.
     * 생성 시점 판단은 Block 도메인이 하고, 실제 INSERT 는 이 도메인이 한다.
     */
    Long create(Long blockId);

    Optional<Settlement> findActiveBySettleId(Long settleId);

    /**
     * 정산 항목 내용을 채운다(작성/수정 겸용) — 행 자체는 Block 도메인이 블록 생성 시점에
     * 이미 빈 상태로 만들어 둔다. {@code encryptedAccountNumber} 는 암호화가 끝난 값을 받는다.
     */
    Settlement updateItem(Long settleId, SettlementType type, Integer roundNo, Long totalAmount,
                           Long plannedAmount, Long plannedTaxAmount, LocalDate plannedDate,
                           String traderName, String bankName, String encryptedAccountNumber,
                           String accountHolder);

    /**
     * @return 실제로 이번 호출이 삭제 처리했으면 true, 이미 삭제돼 있어 아무것도 안 했으면 false
     *         (중복 삭제 이벤트 판별용)
     */
    boolean markDeleted(Long settleId, LocalDateTime deletedAt);
}
