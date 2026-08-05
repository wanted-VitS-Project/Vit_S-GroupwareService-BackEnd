package com.group3.vitamins.text.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * {@code @DynamicUpdate} — 바뀐 컬럼만 UPDATE 문에 넣는다. 기본값(전체 컬럼 UPDATE)이면
 * 본문 수정이 오래된 deletedAt 값까지 SQL 에 실어 보내서, 그 사이 삭제된 행을 되살릴 수 있다.
 */
@Entity
@NoArgsConstructor
@Getter
@DynamicUpdate
@Table(name = "text")
public class TextJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "txt_id")
    private Long txtId;

    // FK 아님. 공용 block 테이블 참조용 값만 저장 (동훈님 쪽에서 채워줌) — 통합 스키마 기준 NOT NULL
    @Column(name = "block_id", nullable = false)
    private Long blockId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 상세 빈 행 생성용. 본문은 나중에 applyContent 가 채운다. */
    public TextJpaEntity(Long blockId) {
        this.blockId = blockId;
    }

    public void applyContent(String content) {
        this.content = content;
    }

    public void applyDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
