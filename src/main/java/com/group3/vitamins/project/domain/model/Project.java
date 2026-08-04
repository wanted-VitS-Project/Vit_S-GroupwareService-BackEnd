package com.group3.vitamins.project.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Project {

    private final Long projectId;
    private final Long bidNoticeId;
    private String name;
    private String description;
    private final ProjectStatus status;
    private String clientName;
    private BigDecimal contractAmount;
    private LocalDate startedOn;
    private LocalDate endedOn;
    private final String createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    private Project(Long projectId, Long bidNoticeId, String name, String description, ProjectStatus status,
                    String clientName, BigDecimal contractAmount, LocalDate startedOn, LocalDate endedOn,
                    String createdBy, LocalDateTime createdAt, LocalDateTime deletedAt) {
        this.projectId = projectId;
        this.bidNoticeId = bidNoticeId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.clientName = clientName;
        this.contractAmount = contractAmount;
        this.startedOn = startedOn;
        this.endedOn = endedOn;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    /** 프로젝트를 생성한다. 상태는 항상 NOT_STARTED. bidNoticeId 는 null 이면 공고 없이 생성된다 (PRJ-001·002). */
    public static Project create(Long bidNoticeId, String name, String description, String clientName,
                                 LocalDate startedOn, LocalDate endedOn, BigDecimal contractAmount,
                                 String createdBy, LocalDateTime now) {
        return new Project(null, bidNoticeId, name, description, ProjectStatus.NOT_STARTED,
                clientName, contractAmount, startedOn, endedOn, createdBy, now, null);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static Project restore(Long projectId, Long bidNoticeId, String name, String description,
                                  ProjectStatus status, String clientName, BigDecimal contractAmount,
                                  LocalDate startedOn, LocalDate endedOn, String createdBy,
                                  LocalDateTime createdAt, LocalDateTime deletedAt) {
        return new Project(projectId, bidNoticeId, name, description, status, clientName, contractAmount,
                startedOn, endedOn, createdBy, createdAt, deletedAt);
    }

    public Long getProjectId() { return projectId; }
    public Long getBidNoticeId() { return bidNoticeId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ProjectStatus getStatus() { return status; }
    public String getClientName() { return clientName; }
    public BigDecimal getContractAmount() { return contractAmount; }
    public LocalDate getStartedOn() { return startedOn; }
    public LocalDate getEndedOn() { return endedOn; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}