package com.group3.vitamins.settlement.application.query;

/** {@code type} 은 쿼리파라미터 원문 그대로 받는다 — enum 파싱·검증은 서비스가 도메인 에러코드로 직접 한다. */
public record SettlementRecommendationQuery(
        Long settleId,
        String type,
        String userId,
        String role
) {
}
