package com.group3.vitamins.issue.infrastructure.adapter;

/** 현재 회사 범위에서 이슈 연결 가능 여부를 검증할 Block 조회 행. */
public record IssueBlockLinkableRow(Long blockId, Long stepId, String title, String type) {
}
