package com.group3.vitamins.project.block.application.result;

/**
 * 블록 이동 결과.
 * unlinkedIssueCount 는 이동 때문에 끊긴 이슈-블록 연결 수다 — FE 가 사용자에게 알려야 한다 (BLK-009 · INV-06).
 *
 * @param version 저장 후의 새 버전
 */
public record BlockMoveResult(Long blockId, Long stepId, int unlinkedIssueCount, int version) {
}
