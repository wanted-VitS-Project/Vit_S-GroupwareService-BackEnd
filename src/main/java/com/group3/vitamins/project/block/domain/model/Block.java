package com.group3.vitamins.project.block.domain.model;

import java.time.LocalDateTime;

public class Block {

    private static final int INITIAL_VERSION = 1;

    private final Long blockId;
    private Long stepId;
    private String title;
    private final BlockType type;
    private Long typeId;
    private String owner;
    private int rowIndex;
    private int colSpan;
    private int sortOrder;
    private final int version;
    private final String createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private Block(Long blockId, Long stepId, String title, BlockType type, Long typeId,
                  String owner, int rowIndex, int colSpan, int sortOrder, int version,
                  String createdBy,
                  LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.blockId = blockId;
        this.stepId = stepId;
        this.title = title;
        this.type = type;
        this.typeId = typeId;
        this.owner = owner;
        this.rowIndex = rowIndex;
        this.colSpan = colSpan;
        this.sortOrder = sortOrder;
        this.version = version;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    /**
     * 블록을 생성한다. typeId 는 상세 행을 넣은 뒤 linkTypeId 로 채운다 (3단계 중 ①).
     * title·owner 는 null 로 시작할 수 있다 — 추가 직후 빈 카드를 허용한다.
     */
    public static Block create(Long stepId, BlockType type, String title, String owner,
                               int rowIndex, int sortOrder, int colSpan,
                               String createdBy, LocalDateTime now) {
        return new Block(null, stepId, title, type, null, owner,
                rowIndex, colSpan, sortOrder, INITIAL_VERSION, createdBy, now, now, null);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static Block restore(Long blockId, Long stepId, String title, BlockType type, Long typeId,
                                String owner, int rowIndex, int colSpan, int sortOrder,
                                int version, String createdBy, LocalDateTime createdAt,
                                LocalDateTime updatedAt, LocalDateTime deletedAt) {
        return new Block(blockId, stepId, title, type, typeId, owner,
                rowIndex, colSpan, sortOrder, version, createdBy, createdAt, updatedAt, deletedAt);
    }

    /** 상세 행 PK 를 연결한다 (3단계 중 ③). 상세 행이 없는 타입은 호출하지 않는다. */
    public void linkTypeId(Long typeId, LocalDateTime now) {
        this.typeId = typeId;
        this.updatedAt = now;
    }

    /** 제목을 바꾼다. null 을 주면 해제다. */
    public void changeTitle(String title, LocalDateTime now) {
        this.title = title;
        this.updatedAt = now;
    }

    /** 담당자를 바꾼다. null 을 주면 해제다. */
    public void changeOwner(String owner, LocalDateTime now) {
        this.owner = owner;
        this.updatedAt = now;
    }

    /** 배치를 옮긴다. 같은 행에 순서가 겹쳐도 허용한다 (BLK-004). */
    public void relocate(int rowIndex, int sortOrder, int colSpan, LocalDateTime now) {
        this.rowIndex = rowIndex;
        this.sortOrder = sortOrder;
        this.colSpan = colSpan;
        this.updatedAt = now;
    }

    /**
     * 다른 스텝으로 옮긴다 (BLK-014). 도착 스텝 기준으로 배치를 다시 잡는다 —
     * 원래 rowIndex 를 그대로 들고 가면 도착 스텝의 기존 블록과 좌표가 겹쳐 그리드가 깨진다.
     */
    public void moveToStep(Long stepId, int rowIndex, int sortOrder, LocalDateTime now) {
        this.stepId = stepId;
        this.rowIndex = rowIndex;
        this.sortOrder = sortOrder;
        this.updatedAt = now;
    }

    /** 논리 삭제한다. 삭제 판정의 주인은 항상 이 값이다 (BLK-007). */
    public void delete(LocalDateTime now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public Long getBlockId() { return blockId; }
    public Long getStepId() { return stepId; }
    public String getTitle() { return title; }
    public BlockType getType() { return type; }
    public Long getTypeId() { return typeId; }
    public String getOwner() { return owner; }
    public int getRowIndex() { return rowIndex; }
    public int getColSpan() { return colSpan; }
    public int getSortOrder() { return sortOrder; }

    /**
     * 조회 시점의 낙관적 락 버전이다 (`.ai/docs/global/CONCURRENCY.md`).
     *
     * <p>⚠️ 도메인은 이 값을 <b>절대 올리지 않는다.</b> {@code +1} 은 {@code WHERE version = ?} 과
     * 같은 UPDATE 문장 안에서 DB 가 한다. 여기서 올리면 덮어쓰기 기대값이 DB+1 이 되어
     * <b>모든 덮어쓰기가 409</b> 가 된다.
     */
    public int getVersion() { return version; }

    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}