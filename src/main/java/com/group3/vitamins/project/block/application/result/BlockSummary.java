package com.group3.vitamins.project.block.application.result;

public record BlockSummary(
        Long blockId,
        String type,
        String title,
        BlockOwner owner,
        int rowIndex,
        int sortOrder,
        int colSpan,
        BlockDetail detail,
        int linkedIssueTotal,
        int linkedIssueDone
) {
}