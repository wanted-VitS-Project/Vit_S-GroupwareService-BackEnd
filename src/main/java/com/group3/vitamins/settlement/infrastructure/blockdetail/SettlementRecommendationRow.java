package com.group3.vitamins.settlement.infrastructure.blockdetail;

/**
 * 정산 항목 수정 화면 진입 시 추천값 계산용 조회 행.
 *
 * @param blockCount 이 정산 블록을 제외한, 같은 프로젝트에 존재하는 활성 정산 블록 개수
 * @param recommendedTotalAmount 같은 프로젝트의 다른 정산 블록 중 이미 total_amount가 채워진 값
 *                                (연결된(status != PENDING) 블록을 최우선으로 고른다). 없으면 null
 */
public record SettlementRecommendationRow(Long blockCount, Long recommendedTotalAmount) {
}
