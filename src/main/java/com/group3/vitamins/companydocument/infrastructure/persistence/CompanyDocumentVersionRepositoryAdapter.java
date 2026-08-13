package com.group3.vitamins.companydocument.infrastructure.persistence;

import com.group3.vitamins.companydocument.domain.model.CompanyDocumentVersion;
import com.group3.vitamins.companydocument.domain.repository.CompanyDocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CompanyDocumentVersionRepositoryAdapter implements CompanyDocumentVersionRepository {

    private final SpringDataCompanyDocumentVersionRepository springDataRepository;

    @Override
    public CompanyDocumentVersion save(CompanyDocumentVersion version) {
        return CompanyDocumentVersionPersistenceMapper.toDomain(
                springDataRepository.saveAndFlush(CompanyDocumentVersionPersistenceMapper.toEntity(version)));
    }

    @Override
    public Optional<CompanyDocumentVersion> findById(Long versionId) {
        return springDataRepository.findById(versionId)
                .map(CompanyDocumentVersionPersistenceMapper::toDomain);
    }

    @Override
    public int findMaxVersionNo(Long companyDocumentId) {
        Integer max = springDataRepository.findMaxVersionNo(companyDocumentId);
        return max == null ? 0 : max;
    }
}
