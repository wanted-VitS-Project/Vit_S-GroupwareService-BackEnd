package com.group3.vitamins.issue.infrastructure.persistence;

import com.group3.vitamins.issue.domain.model.Issue;

public class IssueMapper {

    private IssueMapper() {
    }

    public static Issue toDomain(IssueEntity entity) {
        return Issue.restore(
                entity.getIssueId(),
                entity.getStepId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getDueDate(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getFinishDay(),
                entity.getDeletedAt());
    }

    public static IssueEntity toEntity(Issue issue) {
        return new IssueEntity(
                issue.getIssueId(),
                issue.getTitle(),
                issue.getContent(),
                issue.getDueDate(),
                issue.getStatus(),
                issue.getStepId(),
                null,
                issue.getCompletedAt(),
                issue.getPriority(),
                issue.getCreatedBy(),
                issue.getCreatedAt(),
                issue.getUpdatedAt(),
                issue.getDeletedAt());
    }
}
