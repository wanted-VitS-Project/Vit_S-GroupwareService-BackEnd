package com.group3.vitamins.companydocument.infrastructure.adapter;

import com.group3.vitamins.companydocument.application.port.CompanyDocumentQueryPort;
import com.group3.vitamins.companydocument.application.query.CompanyDocumentListCriteria;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentListProjection;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentVersionProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CompanyDocumentQueryAdapter implements CompanyDocumentQueryPort {

    private final CompanyDocumentQueryMapper mapper;

    @Override
    public long countDocuments(CompanyDocumentListCriteria criteria) {
        return mapper.countDocuments(criteria);
    }

    @Override
    public List<CompanyDocumentListProjection> findDocuments(CompanyDocumentListCriteria criteria) {
        return mapper.findDocuments(criteria);
    }

    @Override
    public List<CompanyDocumentVersionProjection> findCompletedVersions(Long companyDocumentId) {
        return mapper.findCompletedVersions(companyDocumentId);
    }
}
