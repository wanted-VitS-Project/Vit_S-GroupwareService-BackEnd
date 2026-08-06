package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 비타메이트 AI 블록의 타입별 상세 행을 저장하는 JPA 엔티티
@Getter
@Entity
@DynamicUpdate
@Table(
        name = "vitamate_block",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vitamate_block_block", columnNames = "block_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VitamateBlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vitamate_block_id")
    private Long id;

    @Column(name = "block_id", nullable = false)
    private Long blockId;

    @Column(name = "welcome_message", length = 500)
    private String welcomeMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Block 도메인이 AI 블록을 생성할 때 비어 있는 상세 행을 만든다.
    public VitamateBlockEntity(Long blockId) {
        this.blockId = blockId;
    }
}
