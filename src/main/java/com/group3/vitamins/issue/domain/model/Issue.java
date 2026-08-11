package com.group3.vitamins.issue.domain.model;

import com.group3.vitamins.issue.domain.IssuePriority;
import com.group3.vitamins.issue.domain.IssueStatus;

import java.time.LocalDateTime;

public class Issue {

    private final Long issueId;
    private final Long stepId;
    private String title;
    private String content;
    private LocalDateTime dueDate;
    private IssueStatus status;
    private IssuePriority priority;
    private final int version;
    private final String createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime deletedAt;

    private Issue(Long issueId, Long stepId, String title, String content,
                  LocalDateTime dueDate, IssueStatus status, IssuePriority priority, int version,
                  String createdBy, LocalDateTime createdAt, LocalDateTime updatedAt,
                  LocalDateTime completedAt, LocalDateTime deletedAt) {
        this.issueId = issueId;
        this.stepId = stepId;
        this.title = title;
        this.content = content;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
        this.version = version;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
        this.deletedAt = deletedAt;
    }

    public static Issue create(Long stepId, String title, String content, LocalDateTime dueDate,
                               IssueStatus status, IssuePriority priority,
                               String createdBy, LocalDateTime now) {
        IssueStatus resolvedStatus = status == null ? IssueStatus.TO_DO : status;
        return new Issue(null, stepId, title, content, dueDate, resolvedStatus, priority, 1,
                createdBy, now, now, resolvedStatus == IssueStatus.DONE ? now : null, null);
    }

    public static Issue restore(Long issueId, Long stepId, String title, String content,
                                LocalDateTime dueDate, IssueStatus status,
                                IssuePriority priority, int version, String createdBy,
                                LocalDateTime createdAt, LocalDateTime updatedAt,
                                LocalDateTime completedAt, LocalDateTime deletedAt) {
        return new Issue(issueId, stepId, title, content, dueDate, status, priority, version,
                createdBy, createdAt, updatedAt, completedAt, deletedAt);
    }

    public void delete(LocalDateTime now) {
        this.deletedAt = now;
    }

    public void changeStatus(IssueStatus nextStatus, LocalDateTime now) {
        if (this.status == nextStatus) {
            return;
        }
        this.status = nextStatus;
        this.completedAt = nextStatus == IssueStatus.DONE ? now : null;
    }

    public void updateFields(String title, String content, LocalDateTime dueDate, IssuePriority priority) {
        this.title = title;
        this.content = content;
        this.dueDate = dueDate;
        this.priority = priority;
    }

    public Long getIssueId() { return issueId; }
    public Long getStepId() { return stepId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public LocalDateTime getDueDate() { return dueDate; }
    public IssueStatus getStatus() { return status; }
    public IssuePriority getPriority() { return priority; }
    public int getVersion() { return version; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}
