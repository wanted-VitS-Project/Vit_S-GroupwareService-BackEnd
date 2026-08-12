package com.group3.vitamins.file.application.query;

/**
 * 전사 파일 관리 조회 요청(FILE-Q-01) — 컨트롤러가 넘기는 원시 파라미터. 서비스가 검증·변환해 Criteria 로 만든다.
 *
 * @param requesterUserId 요청자 사번 (권한 로그용)
 * @param role            전역 role (ADMIN 검사용)
 * @param keyword         파일명·원본명·업로더 검색어 (nullable)
 * @param projectId       특정 프로젝트로 필터 (nullable)
 * @param extension       확장자로 필터 (nullable)
 * @param page            0-base 페이지
 * @param size            페이지 크기
 */
public record CompanyFileQuery(
        String requesterUserId,
        String role,
        String keyword,
        Long projectId,
        String extension,
        int page,
        int size
) {
}
