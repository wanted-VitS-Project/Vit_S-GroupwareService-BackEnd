package com.group3.vitamins.settlement.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 정산 블록 도메인 모델 — 영속성 프레임워크에 의존하지 않는다.
 *
 * <p>블록 생성·삭제는 Block 도메인이 전부 처리한다. {@code blockId} 는 공용 block 테이블을
 * 참조하는 값만 저장할 뿐 FK 는 아니며, 이 도메인은 그 값을 쓰지 않고 읽기만 한다.
 *
 * <p>{@code accountNumber} 는 암호화된 값을 그대로 들고 있다 — 이 도메인에는 복호화 로직이 없다
 * (마스킹 응답은 요청으로 받은 평문을 그대로 쓰지, 저장값을 복호화해서 만들지 않는다).
 */
public class Settlement {

    private final Long settleId;
    private final Long blockId;
    private final Integer roundNo;
    private final SettlementType type;
    private final SettlementStatus status;
    private final Long totalAmount;
    private final Long plannedAmount;
    private final Long plannedTaxAmount;
    private final LocalDate plannedDate;
    // 세금계산서 기한 — 면세 등 세금계산서를 받지 않는 회차는 null 이다(2026-08-14 신설).
    private final LocalDate taxInvoiceDueDate;
    private final Long actualAmount;
    private final LocalDateTime actualDate;
    private final String traderName;
    private final String bankName;
    private final String accountNumber;
    private final String accountHolder;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;
    // 조회 시점 값. 도메인은 이 값을 절대 올리지 않는다 — +1은 저장 시 WHERE와 같은 문장 안에서
    // DB가 한다(CONCURRENCY.md §3-3).
    private final int version;

    private Settlement(Long settleId, Long blockId, Integer roundNo, SettlementType type, SettlementStatus status,
                        Long totalAmount, Long plannedAmount, Long plannedTaxAmount, LocalDate plannedDate,
                        LocalDate taxInvoiceDueDate, Long actualAmount, LocalDateTime actualDate, String traderName, String bankName,
                        String accountNumber, String accountHolder,
                        LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt, int version) {
        this.settleId = settleId;
        this.blockId = blockId;
        this.roundNo = roundNo;
        this.type = type;
        this.status = status;
        this.totalAmount = totalAmount;
        this.plannedAmount = plannedAmount;
        this.plannedTaxAmount = plannedTaxAmount;
        this.plannedDate = plannedDate;
        this.taxInvoiceDueDate = taxInvoiceDueDate;
        this.actualAmount = actualAmount;
        this.actualDate = actualDate;
        this.traderName = traderName;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.version = version;
    }

    public static Settlement reconstruct(Long settleId, Long blockId, Integer roundNo, SettlementType type,
                                          SettlementStatus status, Long totalAmount, Long plannedAmount,
                                          Long plannedTaxAmount, LocalDate plannedDate, LocalDate taxInvoiceDueDate,
                                          Long actualAmount, LocalDateTime actualDate, String traderName, String bankName,
                                          String accountNumber, String accountHolder,
                                          LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt,
                                          int version) {
        return new Settlement(settleId, blockId, roundNo, type, status, totalAmount, plannedAmount,
                plannedTaxAmount, plannedDate, taxInvoiceDueDate, actualAmount, actualDate, traderName, bankName,
                accountNumber, accountHolder, createdAt, updatedAt, deletedAt, version);
    }

    public Long getSettleId() {
        return settleId;
    }

    public Long getBlockId() {
        return blockId;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public SettlementType getType() {
        return type;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public Long getPlannedAmount() {
        return plannedAmount;
    }

    public Long getPlannedTaxAmount() {
        return plannedTaxAmount;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public LocalDate getTaxInvoiceDueDate() {
        return taxInvoiceDueDate;
    }

    public Long getActualAmount() {
        return actualAmount;
    }

    public LocalDateTime getActualDate() {
        return actualDate;
    }

    public String getTraderName() {
        return traderName;
    }

    public String getBankName() {
        return bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public int getVersion() {
        return version;
    }
}
