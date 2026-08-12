package com.group3.vitamins.file.application.query;

/**
 * 전사 파일 관리 SQL 실행 조건(FILE-Q-01). 회사 스코프(테넌트 경계)는 항상 적용된다.
 *
 * @param companyId  요청자 회사 (테넌트 경계 · 항상 적용)
 * @param keyword    파일명·원본명·업로더 검색어 (nullable)
 * @param projectId  프로젝트 필터 (nullable)
 * @param extension  확장자 필터 (nullable)
 * @param offset     LIMIT 시작 위치 (page * size)
 * @param limit      페이지 크기
 */
public record CompanyFileCriteria(
        Long companyId,
        String keyword,
        Long projectId,
        String extension,
        int offset,
        int limit
) {
}
