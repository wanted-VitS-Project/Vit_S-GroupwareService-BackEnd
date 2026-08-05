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
    private final StepStatus status;
    private final LocalDateTime completedAt;
    private final String completedBy;
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