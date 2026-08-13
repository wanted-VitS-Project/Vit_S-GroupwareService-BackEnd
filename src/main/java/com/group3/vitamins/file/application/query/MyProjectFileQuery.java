package com.group3.vitamins.file.application.query;

/**
 * 내 프로젝트 파일 모아보기 조회 요청(FILE-Q-03) — 컨트롤러가 넘기는 원시 파라미터.
 *
 * @param requesterUserId 요청자 사번 (멤버십·스텝 권한 판정용)
 * @param role            전역 role (ADMIN/MASTER 면 스텝 권한 필터 스킵)
 * @param keyword         파일명·원본명·업로더 검색어 (nullable)
 * @param projectId       특정 프로젝트로 필터 (nullable)
 * @param extension       확장자로 필터 (nullable)
 */
public record MyProjectFileQuery(
        String requesterUserId,
        String role,
        String keyword,
        Long projectId,
        String extension
) {
}
