package com.group3.vitamins.companydocument.application.result;

/**
 * 참조 선택용 사내 문서 뷰 (입찰 검토 비교자료 선택 · COMPANY-DOC-V1 §2-G 연동). 회사 스코프에서 선택 가능한
 * 문서의 <b>최신 완료 버전</b>을 버전 고정({@code companyDocumentVersionId})으로 노출한다.
 *
 * <p>{@code indexStatus} 는 AI 인덱스 준비 상태다 — 현재는 AI 인덱스 상태 테이블이 없어 {@code null}(미추적)이며,
 * §6-2 로 AI 가 테이블을 붙이면 매퍼가 LEFT JOIN + {@code COALESCE(status,'PENDING')} 로 채우고 READY 만 필터한다.
 *
 * <p>⚠️ {@code storageKey} 는 입찰 검토 Worker 가 <b>서버측 presigned URL 발급 전용</b>으로 쓰는 S3 원본 키다.
 * Worker 는 ADMIN 이 아니라 관리 다운로드 API 를 못 쓰므로, {@code findSelectableVersion} 포트로 versionId→storageKey 를
 * 받아 직접 presign 한다(bid_reference_file 과 동일 방식). <b>프론트 응답({@code CompanyDocumentReferenceResponse})에는
 * 노출하지 않는다</b> — 내부 키를 사용자에게 흘리지 않기 위한 정보 최소화(2026-08-13 결정).
 *
 * @param companyDocumentId        문서 번호
 * @param companyDocumentVersionId 참조로 고정할 버전 번호(bid_review_document 가 참조)
 * @param category                 분류 enum
 * @param originalFileName         원본 파일명
 * @param versionNo                버전 차수
 * @param indexStatus              AI 인덱스 준비 상태(현재 null · §6-2 이후 채움)
 * @param storageKey               S3 원본 키 — 입찰 Worker presign 전용, 프론트 응답 미노출
 */
public record CompanyDocumentReferenceView(
        Long companyDocumentId,
        Long companyDocumentVersionId,
        String category,
        String originalFileName,
        int versionNo,
        String indexStatus,
        String storageKey
) {
}
