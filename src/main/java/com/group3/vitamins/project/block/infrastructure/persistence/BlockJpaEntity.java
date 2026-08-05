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

    @Column(name = "created_by", nullable = false, length = 20)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}