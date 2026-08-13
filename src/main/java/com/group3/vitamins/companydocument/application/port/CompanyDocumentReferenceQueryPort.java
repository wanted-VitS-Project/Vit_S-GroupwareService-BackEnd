package com.group3.vitamins.companydocument.application.port;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentReferenceView;

import java.util.List;
import java.util.Optional;

/**
 * 사내 문서 <b>참조 선택용</b> 조회 아웃바운드 포트 (입찰 검토 연동 · COMPANY-DOC-V1 §2-G).
 *
 * <p>관리(ADMIN)용 {@code CompanyDocumentQueryPort} 와 분리한다 — 이쪽은 <b>회사 소속이면 MEMBER 도</b>
 * 참조 자료를 고를 수 있는 저권한 조회다. 회사 스코프·soft delete 제외·완료(COMPLETED) 최신 버전만 노출하며,
 * 참조는 버전 고정({@code companyDocumentVersionId})으로 준다.
 */
public interface CompanyDocumentReferenceQueryPort {

    /** 참조 선택 목록 — 회사 스코프, 완료 최신 버전만. category·keyword 는 선택 필터(null 허용). */
    List<CompanyDocumentReferenceView> findSelectableDocuments(Long companyId, String category, String keyword);

    /** 특정 버전이 이 회사의 참조 대상으로 유효한지(완료·미삭제·문서 미삭제). 참조 생성 검증용. */
    Optional<CompanyDocumentReferenceView> findSelectableVersion(Long companyDocumentVersionId, Long companyId);
}
