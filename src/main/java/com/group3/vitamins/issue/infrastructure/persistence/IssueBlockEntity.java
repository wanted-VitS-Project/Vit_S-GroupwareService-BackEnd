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
        name = "issue_block",
        uniqueConstraints = @UniqueConstraint(name = "uk_ib", columnNames = {"issue_id", "block_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueBlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_block_id")
    private Long issueBlockId;

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "block_id", nullable = false)
    private Long blockId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static IssueBlockEntity link(Long issueId, Long blockId) {
        IssueBlockEntity issueBlock = new IssueBlockEntity();
        issueBlock.issueId = issueId;
        issueBlock.blockId = blockId;
        return issueBlock;
    }
}
