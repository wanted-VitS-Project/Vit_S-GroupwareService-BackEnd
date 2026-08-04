package com.group3.vitamins.approval.application.port;

/** 공용 block 테이블 조회 결과 중 결재 도메인이 필요로 하는 최소 정보 */
public record BlockSummary(Long blockId, String type, Long projectId) {
}
