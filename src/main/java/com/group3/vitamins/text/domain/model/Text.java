package com.group3.vitamins.text.domain.model;

import java.time.LocalDateTime;

/**
 * 텍스트 블록 도메인 모델 — 영속성 프레임워크에 의존하지 않는다.
 *
 * <p>블록 생성·삭제는 Block 도메인이 전부 처리한다. {@code blockId} 는 공용 block 테이블을
 * 참조하는 값만 저장할 뿐 FK 는 아니며, 이 도메인은 그 값을 쓰지 않고 읽기만 한다.
 *
 * <p>불변 읽기 모델이다 — 실제 수정·삭제는 {@link com.group3.vitamins.text.domain.repository.TextRepository}
 * 의 상태별 메서드(updateContent/markDeleted)가 조회 직전에 새로 읽은 엔티티에만 반영한다.
 * 여기서 값을 바꿔 다시 저장하는 방식은 동시 삭제를 되돌릴 수 있어 쓰지 않는다.
 */
public class Text {

    private final Long txtId;
    private final Long blockId;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;
    // 조회 시점 값. 도메인은 이 값을 절대 올리지 않는다 — +1은 저장 시 WHERE와 같은 문장 안에서
    // DB가 한다(CONCURRENCY.md §3-3). 그대로 "덮어쓰기 기대값"으로 쓰인다.
    private final int version;

    private Text(Long txtId, Long blockId, String content,
                  LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt, int version) {
        this.txtId = txtId;
        this.blockId = blockId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.version = version;
    }

    public static Text reconstruct(Long txtId, Long blockId, String content,
                                    LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt,
                                    int version) {
        return new Text(txtId, blockId, content, createdAt, updatedAt, deletedAt, version);
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

    public int getVersion() {
        return version;
    }
}
