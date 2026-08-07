package com.group3.vitamins.project.infrastructure.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code project} 상세 조회 결과 한 행 (평면).
 * 카테고리를 조인해 가져오므로 카테고리 수만큼 행이 나온다 — 접기는 어댑터가 한다.
 *
 * <p>⚠️ 필드 순서 = XML SELECT 컬럼 순서. MyBatis 가 위치로 생성자에 꽂는다.
 */
public record ProjectDetailRow(
        Long projectId,
        String name,
        String description,
        String clientName,
        String status,
        LocalDate startedOn,
        LocalDate endedOn,
        BigDecimal contractAmount,
        int stepCount,
        int doneStepCount,
        Long bidNoticeId,
        String closeReasonCode,
        String closeReasonNote,
        String memberPermission,
        LocalDateTime createdAt,
        Long categoryId,
        String categoryName,
        String categoryCode
) {
}