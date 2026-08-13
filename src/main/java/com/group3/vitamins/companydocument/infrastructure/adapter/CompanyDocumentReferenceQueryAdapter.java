package com.group3.vitamins.companydocument.infrastructure.adapter;

import com.group3.vitamins.companydocument.application.port.CompanyDocumentReferenceQueryPort;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentReferenceView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link CompanyDocumentReferenceQueryPort} 의 MyBatis 어댑터. 실제 SQL 은
 * {@link CompanyDocumentReferenceQueryMapper} 와 그 XML 이 갖는다.
 */
@Component
@RequiredArgsConstructor
public class CompanyDocumentReferenceQueryAdapter implements CompanyDocumentReferenceQueryPort {

    private final CompanyDocumentReferenceQueryMapper mapper;

    @Override
    public List<CompanyDocumentReferenceView> findSelectableDocuments(Long companyId, String category, String keyword) {
        return mapper.findSelectableDocuments(companyId, category, keyword);
    }

    @Override
    public Optional<CompanyDocumentReferenceView> findSelectableVersion(Long companyDocumentVersionId, Long companyId) {
        return mapper.findSelectableVersion(companyDocumentVersionId, companyId);
    }
}
