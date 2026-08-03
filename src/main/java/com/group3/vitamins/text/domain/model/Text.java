package com.group3.vitamins.text.domain.model;

import java.time.LocalDateTime;

/**
 * 텍스트 블록 도메인 모델 — 영속성 프레임워크에 의존하지 않는다.
 *
 * <p>block_id 를 갖지 않는다. 블록 생성·삭제는 Block 도메인이 전부 처리하고,
 * 삭제 시 발행하는 이벤트를 리스너로 받아 이 쪽 데이터만 정리한다.
 */
public class Text {

    private final Long txtId;
    private String content;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private Text(Long txtId, String content,
                  LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.txtId = txtId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Text reconstruct(Long txtId, String content,
                                    LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        return new Text(txtId, content, createdAt, updatedAt, deletedAt);
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
