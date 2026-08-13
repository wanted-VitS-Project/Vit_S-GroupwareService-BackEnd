package com.group3.vitamins.companydocument.application.query;

/**
 * 사내 문서 목록 SQL 실행 조건(§3). 회사 스코프(테넌트 경계)는 항상 적용된다.
 *
 * @param companyId 요청자 회사 (테넌트 경계 · 항상 적용)
 * @param category  카테고리 필터 enum (nullable)
 * @param keyword   표시명·원본명 검색어 (nullable)
 * @param offset    LIMIT 시작 위치 (page * size) — 큰 page 에서 int overflow 를 피하려 long
 * @param limit     페이지 크기
 */
public record CompanyDocumentListCriteria(
        Long companyId,
        String category,
        String keyword,
        long offset,
        int limit
) {
}
