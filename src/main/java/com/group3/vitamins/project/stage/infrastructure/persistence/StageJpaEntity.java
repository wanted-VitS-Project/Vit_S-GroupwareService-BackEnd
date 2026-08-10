package com.group3.vitamins.project.stage.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "stage")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stage_id")
    private Long stageId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /**
     * 낙관적 락 버전 (`.ai/docs/global/CONCURRENCY.md`).
     *
     * <p>⛔ {@code @Version} 을 붙이지 마라. {@code StageMapper.toEntity} 가 매번 {@code new} 로
     * detached 객체를 만들어 JPA 가 {@code merge} 로 처리하는데, merge 는 DB 의 최신 version 을
     * 다시 읽어와 검사하므로 <b>항상 통과한다</b> — 예외도 안 나고 테스트도 통과하는데
     * 유실만 그대로 남는다 (§6-1).
     */
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}