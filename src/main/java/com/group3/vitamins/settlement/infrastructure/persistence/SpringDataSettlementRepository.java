package com.group3.vitamins.settlement.infrastructure.persistence;

import com.group3.vitamins.settlement.domain.model.SettlementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface SpringDataSettlementRepository extends JpaRepository<SettlementJpaEntity, Long> {

    /**
     * deleted_at 조건을 UPDATE 문 자체에 걸어서, "확인 후 쓰기" 2단계 사이의 틈을 없앤다.
     * 이미 삭제된 행이면 0을 반환한다. clearAutomatically 로 벌크 업데이트 후 영속성 컨텍스트의
     * 캐시된(오래된) 엔티티를 지워서, 이어지는 조회가 DB 최신값을 다시 읽게 한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SettlementJpaEntity s SET "
            + "s.type = :type, s.roundNo = :roundNo, s.totalAmount = :totalAmount, "
            + "s.plannedAmount = :plannedAmount, s.plannedTaxAmount = :plannedTaxAmount, "
            + "s.plannedDate = :plannedDate, s.traderName = :traderName, s.bankName = :bankName, "
            + "s.accountNumber = :accountNumber, s.accountHolder = :accountHolder, "
            + "s.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE s.settleId = :settleId AND s.deletedAt IS NULL")
    int updateItemIfActive(@Param("settleId") Long settleId,
                            @Param("type") SettlementType type,
                            @Param("roundNo") Integer roundNo,
                            @Param("totalAmount") BigDecimal totalAmount,
                            @Param("plannedAmount") BigDecimal plannedAmount,
                            @Param("plannedTaxAmount") BigDecimal plannedTaxAmount,
                            @Param("plannedDate") LocalDate plannedDate,
                            @Param("traderName") String traderName,
                            @Param("bankName") String bankName,
                            @Param("accountNumber") String accountNumber,
                            @Param("accountHolder") String accountHolder);

    /**
     * 같은 이유로 삭제도 조건부 UPDATE — 이미 삭제된 행이면 0을 반환한다.
     * 이걸로 중복 삭제 이벤트를 구분하고(0=이미 삭제됨), 동시 삭제 시 최초 삭제 시각이
     * 나중 이벤트로 덮어써지는 것도 막는다(조건에 안 맞으면 애초에 안 씀).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SettlementJpaEntity s SET s.deletedAt = :deletedAt "
            + "WHERE s.settleId = :settleId AND s.deletedAt IS NULL")
    int markDeletedIfActive(@Param("settleId") Long settleId, @Param("deletedAt") LocalDateTime deletedAt);
}
