package com.group3.vitamins.issue.infrastructure.persistence;

import com.group3.vitamins.issue.domain.IssuePriority;
import com.group3.vitamins.issue.domain.IssueStatus;
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
@Table(name = "issue")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_id")
    private Long issueId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "enum('TO_DO','IN_PROGRESS','DONE')")
    private IssueStatus status;

    @Column(name = "step_id", nullable = false, updatable = false)
    private Long stepId;

    @Column(name = "start_day")
    private LocalDateTime startDay;

    @Column(name = "finish_day")
    private LocalDateTime finishDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", columnDefinition = "enum('LOW','MEDIUM','HIGH')")
    private IssuePriority priority;

    @Column(name = "created_by", nullable = false, length = 20, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void changeStatus(IssueStatus nextStatus, LocalDateTime now) {
        if (this.status == nextStatus) {
            return;
        }

        this.status = nextStatus;
        this.finishDay = nextStatus == IssueStatus.DONE ? now : null;
    }

    public void updateFields(String title, String content, LocalDateTime dueDate, IssuePriority priority) {
        this.title = title;
        this.content = content;
        this.dueDate = dueDate;
        this.priority = priority;
    }

    public void delete(LocalDateTime now) {
        this.deletedAt = now;
    }
}
