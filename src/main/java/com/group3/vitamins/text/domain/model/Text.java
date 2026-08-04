package com.group3.vitamins.text.domain.model;

import java.time.LocalDateTime;

/**
 * 텍스트 블록 도메인 모델 — 영속성 프레임워크에 의존하지 않는다.
 *
 * <p>블록 생성·삭제는 Block 도메인이 전부 처리한다. {@code blockId} 는 공용 block 테이블을
 * 참조하는 값만 저장할 뿐 FK 는 아니며, 이 도메인은 그 값을 쓰지 않고 읽기만 한다.
 */
public class Text {

    private final Long txtId;
    private final Long blockId;
    private String content;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private Text(Long txtId, Long blockId, String content,
                  LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.txtId = txtId;
        this.blockId = blockId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Text reconstruct(Long txtId, Long blockId, String content,
                                    LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        return new Text(txtId, blockId, content, createdAt, updatedAt, deletedAt);
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void markDeleted(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getTxtId() {
        return txtId;
    }

    public Long getBlockId() {
        return blockId;
    }

    public String getContent() {
        return content;
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
