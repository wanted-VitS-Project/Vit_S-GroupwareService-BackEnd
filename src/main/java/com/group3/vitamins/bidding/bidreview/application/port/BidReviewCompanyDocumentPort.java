package com.group3.vitamins.bidding.bidreview.application.port;

import java.util.List;

// 사내 문서함(companydocument) 참조 선택 연동 - 김동현의 CompanyDocumentReferenceUseCase를 감싼다.
// ⚠️ 직접 SQL 금지(STATE.md 2026-08-13 결정) - 회사 스코프·완료 최신 버전만 노출하는 규칙을
// 우회하지 않기 위해 항상 이 UseCase를 통해서만 조회한다.
public interface BidReviewCompanyDocumentPort {

    // 검토 생성(세션 경로) 시 선택한 버전이 현재 회사의 참조 대상으로 유효한지 검증한다.
    List<CompanyDocumentReferenceSnapshot> findAccessibleDocuments(
            List<Long> companyDocumentVersionIds
    );

    // Worker 작업 조회(비세션 경로)가 쓰는 단명 다운로드 URL 발급.
    List<DownloadableCompanyDocument> findDownloadableDocuments(
            Long companyId,
            List<Long> companyDocumentVersionIds
    );

    record CompanyDocumentReferenceSnapshot(
            Long companyDocumentVersionId,
            String fileName
    ) {
    }

    record DownloadableCompanyDocument(
            Long companyDocumentVersionId,
            String fileName,
            String downloadUrl
    ) {
    }
}
