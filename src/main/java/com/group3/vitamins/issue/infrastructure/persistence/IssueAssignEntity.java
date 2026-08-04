package com.group3.vitamins.issue.infrastructure.persistence;

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

import java.time.LocalDateTime;

@Entity
@Table(
        name = "issue_assign",
        uniqueConstraints = @UniqueConstraint(name = "UK_issue_assign", columnNames = {"issue_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueAssignEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_assign_id")
    private Long issueAssignId;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static IssueAssignEntity link(Long issueId, String userId) {
        IssueAssignEntity assign = new IssueAssignEntity();
        assign.issueId = issueId;
        assign.userId = userId;
        return assign;
    }
}
