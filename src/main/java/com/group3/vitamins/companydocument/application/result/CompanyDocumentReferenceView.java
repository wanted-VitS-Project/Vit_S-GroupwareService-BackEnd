package com.group3.vitamins.companydocument.application.result;

/**
 * 참조 선택용 사내 문서 뷰 (입찰 검토 비교자료 선택 · COMPANY-DOC-V1 §2-G 연동). 회사 스코프에서 선택 가능한
 * 문서의 <b>최신 완료 버전</b>을 버전 고정({@code companyDocumentVersionId})으로 노출한다.
 *
 * <p>{@code indexStatus} 는 AI 인덱스 준비 상태다 — 현재는 AI 인덱스 상태 테이블이 없어 {@code null}(미추적)이며,
 * §6-2 로 AI 가 테이블을 붙이면 매퍼가 LEFT JOIN + {@code COALESCE(status,'PENDING')} 로 채우고 READY 만 필터한다.
 *
 * @param companyDocumentId        문서 번호
 * @param companyDocumentVersionId 참조로 고정할 버전 번호(bid_review_document 가 참조)
 * @param category                 분류 enum
 * @param originalFileName         원본 파일명
 * @param versionNo                버전 차수
 * @param indexStatus              AI 인덱스 준비 상태(현재 null · §6-2 이후 채움)
 */
public record CompanyDocumentReferenceView(
        Long companyDocumentId,
        Long companyDocumentVersionId,
        String category,
        String originalFileName,
        int versionNo,
        String indexStatus
) {
}
