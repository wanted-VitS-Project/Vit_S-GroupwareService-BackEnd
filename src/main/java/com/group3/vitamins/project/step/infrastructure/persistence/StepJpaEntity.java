package com.group3.vitamins.project.step.infrastructure.persistence;

import com.group3.vitamins.project.step.domain.model.StepStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "step")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StepJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "step_id")
    private Long stepId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "stage_id")
    private Long stageId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /**
     * 낙관적 락 버전 (`.ai/docs/global/CONCURRENCY.md`).
     *
     * <p>⛔ {@code @Version} 을 붙이지 마라. {@code StepMapper.toEntity} 가 매번 {@code new} 로
     * detached 객체를 만들어 JPA 가 {@code merge} 로 처리하는데, merge 는 DB 의 최신 version 을
     * 다시 읽어와 검사하므로 <b>항상 통과한다</b> (§6-1).
     */
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "started_on")
    private LocalDate startedOn;

    @Column(name = "ended_on")
    private LocalDate endedOn;

    @Column(name = "owner_user_id", length = 20)
    private String ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StepStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by", length = 20)
    private String completedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}