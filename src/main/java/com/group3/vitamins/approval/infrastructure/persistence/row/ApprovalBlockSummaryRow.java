package com.group3.vitamins.approval.infrastructure.persistence.row;

/**
 * 결재 블록의 소속 정보 1행 — {@code block} → {@code step} 을 조인 한 번으로 읽는다.
 *
 * <p>{@code block.project_id} 컬럼이 폐기돼 있어 {@code step} 을 거쳐야 {@code projectId} 를 얻는다.
 * 예전에는 이 때문에 블록 조회 1회가 실제로는 쿼리 2발이었다.
 */
public record ApprovalBlockSummaryRow(
        Long blockId,
        String blockType,
        Long stepId,
        Long projectId,
        String createdBy
) {
}
