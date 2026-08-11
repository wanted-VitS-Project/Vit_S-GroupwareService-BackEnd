package com.group3.vitamins.approval.application.port;

/**
 * 공용 block 테이블 조회 결과 중 결재 도메인이 필요로 하는 최소 정보.
 *
 * <p>{@code createdBy}는 결재 상세 자동 생성(APR-001, {@code ApprovalHandlerService.create})이
 * 기안자를 정할 때 쓴다 — {@code BlockDetailPort.createDetail(Long blockId)}엔 블록을 만든 사람이
 * 안 넘어와서, 유일한 출처인 {@code block.created_by}를 이 포트로 가져온다.
 */
public record BlockSummary(Long blockId, String type, Long stepId, Long projectId, String createdBy) {
}
