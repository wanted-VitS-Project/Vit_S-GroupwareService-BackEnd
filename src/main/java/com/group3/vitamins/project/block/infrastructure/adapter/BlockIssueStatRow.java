package com.group3.vitamins.project.block.infrastructure.adapter;

/** issue_block 집계 행. */
public record BlockIssueStatRow(Long blockId, int totalCount, int doneCount) {
}