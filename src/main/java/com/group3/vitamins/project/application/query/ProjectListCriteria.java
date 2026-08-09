package com.group3.vitamins.project.application.query;

import java.time.LocalDate;

/**
 * 프로젝트 목록 조회의 SQL 실행 조건. 서비스가 {@link ProjectListQuery} 를 검증·변환한 결과다.
 *
 * @param requesterUserId 요청자 사번 (참여 여부 판정용)
 * @param adminAll        true 면 참여 여부를 보지 않고 전 건을 조회한다
 * @param offset          LIMIT 시작 위치 (page * size)
 * @param limit           페이지 크기
 */
public record ProjectListCriteria(
        String requesterUserId,
        boolean adminAll,
        String status,
        Long businessCategoryId,
        LocalDate startedOnFrom,
        LocalDate startedOnTo,
        String keyword,
        int offset,
        int limit
) {
}