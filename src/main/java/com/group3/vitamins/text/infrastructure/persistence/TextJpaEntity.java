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

    // ⚠️ @Version(JPA)을 붙이지 않는다 — TextMapper.toEntity가 매번 new로 detached 객체를
    // 만들어 merge되므로 JPA 낙관락은 DB 최신값을 다시 읽어 항상 통과해버린다(CONCURRENCY.md §6-1).
    // 수동 WHERE version = ? 조건부 UPDATE로만 검사한다.
    @Column(name = "version", nullable = false)
    private int version;

    /**
     * 상세 빈 행 생성용. 본문은 나중에 applyContent 가 채운다.
     *
     * ⚠️ version을 명시적으로 1로 채운다 — Java int 필드 기본값은 0이라, 이 생성자로 만든 새 엔티티를
     * 그대로 save()하면 컬럼의 {@code DEFAULT 1}과 무관하게 INSERT 문에 0이 그대로 실린다
     * (CONCURRENCY.md §3-1 "기존 행은 전부 1로 시작해야 프론트가 받은 값과 맞물린다"가 신규 행에도 적용됨).
     */
    public TextJpaEntity(Long blockId) {
        this.blockId = blockId;
        this.version = 1;
    }

    public void applyContent(String content) {
        this.content = content;
    }

    public void applyDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
