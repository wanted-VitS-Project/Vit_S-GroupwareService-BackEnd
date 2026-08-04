package com.group3.vitamins.checklist.domain.model;

import java.time.LocalDateTime;

/**
 * 체크리스트 항목 도메인 모델 — 영속성 프레임워크에 의존하지 않는다.
 *
 * <p>체크리스트 블록({@code checklist_block}) 생성·삭제는 Block 도메인이 처리한다. {@code chkBlockId} 는
 * 그 블록 상세 행을 참조하는 값만 저장할 뿐 FK 는 아니며, 이 도메인은 그 값을 읽기 전용으로 쓴다.
 *
 * <p>불변 읽기 모델이다 — 실제 수정·삭제는 {@link com.group3.vitamins.checklist.domain.repository.ChecklistRepository}
 * 의 상태별 메서드(updateFields/markDeleted)가 조회 직전에 새로 읽은 엔티티에만 반영한다.
 */
public class ChecklistItem {

    private final Long chkId;
    private final Long chkBlockId;
    private final String content;
    private final boolean completed;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;

    private ChecklistItem(Long chkId, Long chkBlockId, String content, boolean completed,
                           LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.chkId = chkId;
        this.chkBlockId = chkBlockId;
        this.content = content;
        this.completed = completed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static ChecklistItem reconstruct(Long chkId, Long chkBlockId, String content, boolean completed,
                                             LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        return new ChecklistItem(chkId, chkBlockId, content, completed, createdAt, updatedAt, deletedAt);
    }

    public Long getChkId() {
        return chkId;
    }

    public Long getChkBlockId() {
        return chkBlockId;
    }

    public String getContent() {
        return content;
    }

    public boolean isCompleted() {
        return completed;
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
}
