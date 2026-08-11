package com.group3.vitamins.project.block.infrastructure.persistence;

import com.group3.vitamins.project.block.domain.model.BlockType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "block")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id")
    private Long blockId;

    @Column(name = "step_id", nullable = false)
    private Long stepId;

    @Column(name = "title", length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BlockType type;

    @Column(name = "type_id")
    private Long typeId;

    @Column(name = "owner", length = 20)
    private String owner;

    @Column(name = "row_index", nullable = false)
    private int rowIndex;

    @Column(name = "col_span", nullable = false)
    private int colSpan;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /**
     * 낙관적 락 버전 (`.ai/docs/global/CONCURRENCY.md`).
     *
     * <p>⛔ {@code @Version} 을 붙이지 마라. {@code BlockMapper.toEntity} 가 매번 {@code new} 로
     * detached 객체를 만들어 JPA 가 {@code merge} 로 처리하는데, merge 는 DB 의 최신 version 을
     * 다시 읽어와 검사하므로 <b>항상 통과한다</b> (§6-1).
     */
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_by", nullable = false, length = 20)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}