package com.group3.vitamins.settlement.domain.model;

/**
 * 금액 기반 진행률(paidAmountRatio) 계산 — 이 블록 하나의 값이 아니라 **같은 프로젝트 · 같은 타입**
 * (INCOME/OUTCOME)의 활성 정산 블록 전체에 걸친 actual_amount 합계를, 그 타입의 프로젝트 총 예정 금액
 * (totalAmount)으로 나눈 값이다. INCOME 블록은 입금 진행률을, OUTCOME 블록은 외주 출금 진행률을 보여준다.
 */
public final class SettlementProgress {

    private SettlementProgress() {
    }

    public static double ratio(Long actualAmountSum, Long totalAmount) {
        if (actualAmountSum == null || totalAmount == null || totalAmount == 0) {
            return 0.0;
        }
        return actualAmountSum.doubleValue() / totalAmount.doubleValue();
    }
}
