package com.group3.vitamins.settlement.application.port;

import com.group3.vitamins.settlement.domain.model.SettlementType;

/**
 * 정산 항목 작성/수정(PATCH)·추천 조회(GET)가 필요로 하는, "같은 프로젝트의 형제 정산 블록"을 훑는
 * 조회 전용 포트. 구현체(infrastructure)가 {@code SettlementSiblingMapper}(MyBatis)를 감싼다 —
 * application이 매퍼·Row 타입을 직접 알지 않게 한다(2026-08-09, CodeRabbit 리뷰로 발견된 계층 위반 정리).
 */
public interface SettlementSiblingLookupPort {

    /**
     * 같은 프로젝트·같은 타입(INCOME/OUTCOME)의 활성 정산 블록 전체에 걸친 actual_amount 합계.
     * paidAmountRatio 계산에 쓴다 — 없으면 null(호출자가 0으로 취급).
     */
    Long findActualAmountSum(Long settleId, SettlementType type);

    /**
     * SETL-008 검증용 — 같은 프로젝트·같은 타입의 다른 정산 블록이 이미 정해둔 total_amount.
     * 아직 아무 회차도 정한 적 없으면 null.
     */
    Long findEstablishedTotalAmount(Long settleId, SettlementType type);

    /**
     * SETL-008 검증 직전에 호출한다 — 이 settleId가 속한 프로젝트의 활성 정산 블록 전체를 잠가서,
     * 같은 프로젝트의 서로 다른 정산 블록을 동시에 수정해도 기준값 조회가 최신 커밋 상태를 보게 한다.
     */
    void lockSiblingSettlementBlocksForUpdate(Long settleId);

    /** 추천 회차 번호·추천 총 금액 계산용 조회. 대상이 없으면 null. */
    SiblingRecommendation findSiblingRecommendation(Long settleId, SettlementType type);

    record SiblingRecommendation(Long blockCount, Long recommendedTotalAmount) {
    }
}
