package com.group3.vitamins.project.step.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Step {

    private final Long stepId;
    private final Long projectId;
    private Long stageId;
    private String name;
    private int sortOrder;
    private LocalDate startedOn;
    private LocalDate endedOn;
    private String ownerUserId;
    private StepStatus status;
    private LocalDateTime completedAt;
    private String completedBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private Step(Long stepId, Long projectId, Long stageId, String name, int sortOrder,
                 LocalDate startedOn, LocalDate endedOn, String ownerUserId, StepStatus status,
                 LocalDateTime completedAt, String completedBy,
                 LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.stepId = stepId;
        this.projectId = projectId;
        this.stageId = stageId;
        this.name = name;
        this.sortOrder = sortOrder;
        this.startedOn = startedOn;
        this.endedOn = endedOn;
        this.ownerUserId = ownerUserId;
        this.status = status;
        this.completedAt = completedAt;
        this.completedBy = completedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    /**
     * 스텝을 생성한다. projectId 는 시스템이 항상 채우고(INV-02) 상태는 NOT_STARTED 로 시작한다.
     * stageId 가 null 이면 미소속 스텝이다.
     */
    public static Step create(Long projectId, Long stageId, String name, int sortOrder,
                              LocalDate startedOn, LocalDate endedOn, String ownerUserId,
                              LocalDateTime now) {
        return new Step(null, projectId, stageId, name, sortOrder, startedOn, endedOn,
                ownerUserId, StepStatus.NOT_STARTED, null, null, now, now, null);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static Step restore(Long stepId, Long projectId, Long stageId, String name, int sortOrder,
                               LocalDate startedOn, LocalDate endedOn, String ownerUserId,
                               StepStatus status, LocalDateTime completedAt, String completedBy,
                               LocalDateTime createdAt, LocalDateTime updatedAt,
                               LocalDateTime deletedAt) {
        return new Step(stepId, projectId, stageId, name, sortOrder, startedOn, endedOn,
                ownerUserId, status, completedAt, completedBy, createdAt, updatedAt, deletedAt);
    }


    /**
     * 이름·기간·책임자를 갈아끼운다 (STP-001·003).
     * 위치(소속 스테이지·정렬순서)는 여기서 안 바꾼다 — 순서 변경 API 가 전담한다.
     */
    public Step update(String name, LocalDate startedOn, LocalDate endedOn,
                       String ownerUserId, LocalDateTime now) {
        this.name = name;
        this.startedOn = startedOn;
        this.endedOn = endedOn;
        this.ownerUserId = ownerUserId;
        this.updatedAt = now;
        return this;
    }

    /**
     * 위치를 옮긴다 (STP-002). stageId 가 null 이면 미소속으로 뺀다.
     * 선행 스텝 완료 여부는 보지 않는다.
     */
    public Step moveTo(Long stageId, int sortOrder, LocalDateTime now) {
        this.stageId = stageId;
        this.sortOrder = sortOrder;
        this.updatedAt = now;
        return this;
    }

    /**
     * 상태를 바꾼다 (STP-004). DONE 은 이 경로로 오지 않는다 —
     * 완료는 미완료 이슈 처리 선택이 필요해 {@link #complete} 가 전담한다.
     */
    public Step changeStatus(StepStatus status, LocalDateTime now) {
        this.status = status;
        this.updatedAt = now;
        return this;
    }

    /** 완료 처리한다 (STP-005). 완료자·완료시각을 함께 기록한다. */
    public Step complete(String completedBy, LocalDateTime now) {
        this.status = StepStatus.DONE;
        this.completedBy = completedBy;
        this.completedAt = now;
        this.updatedAt = now;
        return this;
    }

    /**
     * 논리 삭제한다 (INV-05 · STP-013). 하위 블록·이슈 정리는 서비스가 맡는다 —
     * 다른 애그리게이트라 도메인 객체가 손댈 수 없다.
     */
    public Step delete(LocalDateTime now) {
        this.deletedAt = now;
        this.updatedAt = now;
        return this;
    }

    public Long getStepId() { return stepId; }
    public Long getProjectId() { return projectId; }
    public Long getStageId() { return stageId; }
    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }
    public LocalDate getStartedOn() { return startedOn; }
    public LocalDate getEndedOn() { return endedOn; }
    public String getOwnerUserId() { return ownerUserId; }
    public StepStatus getStatus() { return status; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getCompletedBy() { return completedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}