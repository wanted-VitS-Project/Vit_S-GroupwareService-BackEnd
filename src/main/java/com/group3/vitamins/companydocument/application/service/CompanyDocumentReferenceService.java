package com.group3.vitamins.companydocument.application.service;

import com.group3.vitamins.companydocument.application.port.CompanyDocumentReferenceQueryPort;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentReferenceView;
import com.group3.vitamins.companydocument.application.usecase.CompanyDocumentReferenceUseCase;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 사내 문서 참조 선택 조회 서비스. 관리(ADMIN)와 달리 정책 게이트가 없다 — 회사 소속 사용자면 참조 자료를 고를 수 있다.
 * 회사 스코프만 강제(다른 회사 문서는 조회되지 않는다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyDocumentReferenceService implements CompanyDocumentReferenceUseCase {

    private final CompanyDocumentReferenceQueryPort referenceQueryPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public List<CompanyDocumentReferenceView> listSelectable(String category, String keyword) {
        return referenceQueryPort.findSelectableDocuments(
                currentCompanyIdProvider.currentCompanyId(), normalize(category), normalize(keyword));
    }

    @Override
    public Optional<CompanyDocumentReferenceView> getSelectableVersion(Long companyDocumentVersionId) {
        return referenceQueryPort.findSelectableVersion(
                companyDocumentVersionId, currentCompanyIdProvider.currentCompanyId());
    }

    /** 앞뒤 공백 제거 후 빈 문자열은 null 로 눕힌다(필터 미적용과 동일 취급). */
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
