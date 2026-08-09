package com.group3.vitamins.project.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Project {

    private final Long projectId;
    private final Long bidNoticeId;
    private String name;
    private String description;
    private ProjectStatus status;
    private String clientName;
    private BigDecimal contractAmount;
    private LocalDate startedOn;
    private LocalDate endedOn;
    private CloseReasonCode closeReasonCode;
    private String closeReasonNote;
    private LocalDateTime closedAt;
    private final String createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private Project(Long projectId, Long bidNoticeId, String name, String description, ProjectStatus status,
                    String clientName, BigDecimal contractAmount, LocalDate startedOn, LocalDate endedOn,
                    CloseReasonCode closeReasonCode, String closeReasonNote, LocalDateTime closedAt,
                    String createdBy, LocalDateTime createdAt, LocalDateTime updatedAt,
                    LocalDateTime deletedAt) {
        this.projectId = projectId;
        this.bidNoticeId = bidNoticeId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.clientName = clientName;
        this.contractAmount = contractAmount;
        this.startedOn = startedOn;
        this.endedOn = endedOn;
        this.closeReasonCode = closeReasonCode;
        this.closeReasonNote = closeReasonNote;
        this.closedAt = closedAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    /** 프로젝트를 생성한다. 상태는 항상 NOT_STARTED. bidNoticeId 는 null 이면 공고 없이 생성된다 (PRJ-001·002). */
    public static Project create(Long bidNoticeId, String name, String description, String clientName,
                                 LocalDate startedOn, LocalDate endedOn, BigDecimal contractAmount,
                                 String createdBy, LocalDateTime now) {
        return new Project(null, bidNoticeId, name, description, ProjectStatus.NOT_STARTED,
                clientName, contractAmount, startedOn, endedOn, null, null, null,
                createdBy, now, now, null);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static Project restore(Long projectId, Long bidNoticeId, String name, String description,
                                  ProjectStatus status, String clientName, BigDecimal contractAmount,
                                  LocalDate startedOn, LocalDate endedOn,
                                  CloseReasonCode closeReasonCode, String closeReasonNote,
                                  LocalDateTime closedAt, String createdBy,
                                  LocalDateTime createdAt, LocalDateTime updatedAt,
                                  LocalDateTime deletedAt) {
        return new Project(projectId, bidNoticeId, name, description, status, clientName, contractAmount,
                startedOn, endedOn, closeReasonCode, closeReasonNote, closedAt, createdBy,
                createdAt, updatedAt, deletedAt);
    }

    /**
     * 수정 가능한 6필드를 갈아끼운다. "생략" 판정은 호출부가 끝내고 최종 값만 넘긴다.
     * 상태·종결사유·공고연결은 여기서 바꾸지 않는다 — 전용 API 소관이다 (PRJ-006).
     */
    public Project update(String name, String description, String clientName,
                          LocalDate startedOn, LocalDate endedOn, BigDecimal contractAmount,
                          LocalDateTime now) {
        this.name = name;
        this.description = description;
        this.clientName = clientName;
        this.startedOn = startedOn;
        this.endedOn = endedOn;
        this.contractAmount = contractAmount;
        this.updatedAt = now;
        return this;
    }

    /** 상태를 바꾼다. 역방향 전이도 허용한다 (PRJ-003) — 되돌릴 일이 실제로 있다. */
    public Project changeStatus(ProjectStatus status, LocalDateTime now) {
        this.status = status;
        this.updatedAt = now;
        return this;
    }

    /**
     * 사유를 붙여 종결한다. 어느 상태에서든 허용된다 (PRJ-004).
     * 종결해도 목록·로그에서 사라지지 않는다 — 삭제와는 다른 동작이다.
     */
    public Project close(CloseReasonCode closeReasonCode, String closeReasonNote, LocalDateTime now) {
        this.status = ProjectStatus.CLOSED;
        this.closeReasonCode = closeReasonCode;
        this.closeReasonNote = closeReasonNote;
        this.closedAt = now;
        this.updatedAt = now;
        return this;
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
    public CloseReasonCode getCloseReasonCode() { return closeReasonCode; }
    public String getCloseReasonNote() { return closeReasonNote; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}
