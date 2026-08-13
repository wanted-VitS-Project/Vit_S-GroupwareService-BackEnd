package com.group3.vitamins.companydocument.application.usecase;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentReferenceView;

import java.util.List;
import java.util.Optional;

/**
 * 사내 문서 참조 선택 인바운드 유스케이스 (입찰 검토 연동). 회사 소속 사용자(MEMBER 포함)가 호출한다.
 * 회사 스코프는 서비스가 현재 세션 회사로 채운다.
 */
public interface CompanyDocumentReferenceUseCase {

    /** 참조 선택 목록(현재 회사, 완료 최신 버전만). */
    List<CompanyDocumentReferenceView> listSelectable(String category, String keyword);

    /** 특정 버전이 현재 회사의 참조 대상으로 유효한지 조회한다(입찰 귀속 검증용). */
    Optional<CompanyDocumentReferenceView> getSelectableVersion(Long companyDocumentVersionId);
}
