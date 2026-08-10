package com.group3.vitamins.project.infrastructure.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code project} 목록 조회 결과 한 행 (평면).
 * 카테고리·참여자를 조인해 가져오므로 한 프로젝트가 여러 행으로 나온다 — 접기는 어댑터가 한다.
 *
 * <p>⚠️ 필드 순서 = XML SELECT 컬럼 순서. MyBatis 가 위치로 생성자에 꽂는다.
 */
public record ProjectListRow(
        Long projectId,
        String name,
        String clientName,
        String status,
        LocalDate startedOn,
        LocalDate endedOn,
        BigDecimal contractAmount,
        int totalStepCount,
        int doneStepCount,
        int myIssueInProgressCount,
        int myApprovalInProgressCount,
        Long categoryId,
        String categoryName,
        String categoryCode,
        boolean categoryDeleted,
        String memberUserId,
        String memberName,
        boolean memberDeleted
) {
}