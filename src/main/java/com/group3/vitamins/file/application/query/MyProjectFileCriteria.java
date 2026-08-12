package com.group3.vitamins.file.application.query;

/**
 * 내 프로젝트 파일 모아보기 SQL 실행 조건(FILE-Q-03 · 스텝 권한 노출 B안).
 *
 * <p>멤버십(project_member)은 <b>항상</b> 적용된다 — "내 프로젝트" 정의라 ADMIN 도 자기 멤버 프로젝트만 본다.
 * {@code adminAll} 은 <b>스텝 권한 필터만</b> 스킵한다(전역 ADMIN/MASTER 는 스텝 정책상 EDITOR 라 override 무관).
 *
 * @param companyId       요청자 회사 (테넌트 경계 · 항상 적용)
 * @param requesterUserId 요청자 사번 (멤버십·스텝 override 조인 키)
 * @param adminAll        true 면 스텝 권한(step_permission) 필터를 스킵한다. 멤버십 필터는 그대로 적용
 * @param keyword         파일명·원본명·업로더 검색어 (nullable)
 * @param projectId       프로젝트 필터 (nullable)
 * @param extension       확장자 필터 (nullable)
 */
public record MyProjectFileCriteria(
        Long companyId,
        String requesterUserId,
        boolean adminAll,
        String keyword,
        Long projectId,
        String extension
) {
}
