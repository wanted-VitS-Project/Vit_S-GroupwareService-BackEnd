package com.group3.vitamins.companydocument.application.usecase;

import com.group3.vitamins.companydocument.application.query.CompanyDocumentListQuery;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentDownloadResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentPageResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentPreviewResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentVersionHistoryResult;

/** 사내 문서 조회 인바운드 포트 (§3 목록 · §7 버전 이력 · §8 다운로드 · §9 미리보기). 모두 ADMIN 전용. */
public interface CompanyDocumentQueryUseCase {

    /** §3 문서 목록 — 회사 스코프 페이지 조회. 카테고리·검색어 필터. */
    CompanyDocumentPageResult getDocuments(CompanyDocumentListQuery query);

    /** §7 버전 이력 — 완료 버전만 차수 내림차순. */
    CompanyDocumentVersionHistoryResult getVersionHistory(Long companyDocumentId, String requesterUserId, String role);

    /** §8 다운로드 URL 발급 — presigned GET(5분). */
    CompanyDocumentDownloadResult getDownloadUrl(Long versionId, String requesterUserId, String role);

    /** §9 미리보기 — 앞 5페이지만 남긴 PDF 바이너리. */
    CompanyDocumentPreviewResult getPreview(Long versionId, String requesterUserId, String role);
}
