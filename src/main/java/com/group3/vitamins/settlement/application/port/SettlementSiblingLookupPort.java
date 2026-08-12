package com.group3.vitamins.settlement.application.port;

import com.group3.vitamins.settlement.domain.model.SettlementType;

import java.time.LocalDateTime;

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

    /**
     * {@code lockSiblingSettlementBlocksForUpdate} 직후 호출한다 — 이미 잠근 이 settleId 행의
     * **현재(최신 커밋) 상태**를 {@code deletedAt}/{@code status}/{@code version} 그대로 다시 읽는다.
     * FOR UPDATE라 이미 걸린 잠금을 재확인할 뿐 대기 없이 즉시 반환되고, 이후 이 트랜잭션이 끝날 때까지
     * 이 행은 아무도 못 바꾼다 — 그래서 이 조회 결과를 기준으로 삭제·연결 여부를 판정하면 그 뒤의
     * 조건부 UPDATE가 0건이 돼도 원인이 "버전 불일치"뿐임이 보장된다(CodeRabbit, 2026-08-12 —
     * REPEATABLE READ 스냅샷 때문에 갱신 실패 후 일반 조회로 원인을 재분류하면 부정확할 수 있다는
     * 지적을 "실패 후 재분류" 대신 "쓰기 전에 잠금 하에 미리 확정"으로 근본 해결).
     */
    SettlementCurrentState findCurrentStateForUpdate(Long settleId);

    record SettlementCurrentState(Integer version, String status, LocalDateTime deletedAt) {
    }

    /** 추천 회차 번호·추천 총 금액 계산용 조회. 대상이 없으면 null. */
    SiblingRecommendation findSiblingRecommendation(Long settleId, SettlementType type);

    record SiblingRecommendation(Long maxRoundNo, Long recommendedTotalAmount) {
    }
}
