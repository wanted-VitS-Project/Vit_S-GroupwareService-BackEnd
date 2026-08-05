package com.group3.vitamins.issue.infrastructure.adapter;

public record IssueRelatedBlockRow(
        Long issueId,
        Long blockId,
        String title,
        String type
) {
}
