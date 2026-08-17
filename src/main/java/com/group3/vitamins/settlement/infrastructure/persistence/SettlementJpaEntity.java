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

    // block->step 을 타야 알 수 있는 소속 프로젝트를 생성 시점에 스탬핑해 둔 값(V20260816170100).
    // 프로젝트 간 블록 이동이 막혀 있어 생성 후 바뀌지 않는다 — updatable=false 로 못 박는다.
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

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

    // 세금계산서 기한 — 면세 등 세금계산서를 받지 않는 회차는 null(2026-08-14 신설).
    // planned_date(입출금 기한)와 의미가 다르다 — 세금계산서 매칭 추천의 일자 비교가 이 값을 본다.
    @Column(name = "tax_invoice_due_date")
    private LocalDate taxInvoiceDueDate;

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

    // ⚠️ @Version(JPA)을 붙이지 않는다 — SettlementRepositoryAdapter가 매번 new로 detached 객체를
    // 만들어 merge되므로 JPA 낙관락은 DB 최신값을 다시 읽어 항상 통과해버린다(CONCURRENCY.md §6-1).
    @Column(name = "version", nullable = false)
    private int version;

    /**
     * 상세 빈 행 생성용. 내용은 나중에 정산 항목 작성/수정 API 가 채운다.
     *
     * ⚠️ version을 명시적으로 1로 채운다 — Java int 필드 기본값 0을 그대로 두면 컬럼의
     * {@code DEFAULT 1}과 무관하게 INSERT 문에 0이 실린다(CONCURRENCY.md §3-1).
     */
    public SettlementJpaEntity(Long blockId, Long projectId) {
        this.blockId = blockId;
        this.projectId = projectId;
        this.status = SettlementStatus.PENDING;
        this.version = 1;
    }
}
