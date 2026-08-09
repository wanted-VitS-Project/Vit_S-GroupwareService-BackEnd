package com.group3.vitamins.settlement.infrastructure.persistence;

import com.group3.vitamins.settlement.domain.model.SettlementStatus;
import com.group3.vitamins.settlement.domain.model.SettlementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code @DynamicUpdate} — 바뀐 컬럼만 UPDATE 문에 넣는다 (text/checklist 도메인과 동일한 이유:
 * 오래된 deletedAt 값을 실어 보내 동시 삭제를 되살리는 것을 막는다).
 */
@Entity
@NoArgsConstructor
@Getter
@DynamicUpdate
@Table(name = "settlement_block")
public class SettlementJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settle_id")
    private Long settleId;

    // FK 아님. 공용 block 테이블 참조용 값만 저장 (Block 도메인 쪽에서 채워줌) — NOT NULL
    @Column(name = "block_id", nullable = false)
    private Long blockId;

    @Column(name = "round_no")
    private Integer roundNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private SettlementType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SettlementStatus status;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "planned_amount", precision = 18, scale = 2)
    private BigDecimal plannedAmount;

    @Column(name = "planned_tax_amount", precision = 18, scale = 2)
    private BigDecimal plannedTaxAmount;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(name = "actual_amount", precision = 18, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "actual_date")
    private LocalDateTime actualDate;

    @Column(name = "trader_name", length = 250)
    private String traderName;

    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "account_number", length = 250)
    private String accountNumber;

    @Column(name = "account_holder", length = 100)
    private String accountHolder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 상세 빈 행 생성용. 내용은 나중에 정산 항목 작성/수정 API 가 채운다. */
    public SettlementJpaEntity(Long blockId) {
        this.blockId = blockId;
        this.status = SettlementStatus.PENDING;
    }
}
