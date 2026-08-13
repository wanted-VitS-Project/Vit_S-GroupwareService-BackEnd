package com.group3.vitamins.companydocument.infrastructure.persistence;

import com.group3.vitamins.companydocument.domain.model.CompanyDocument;
import com.group3.vitamins.companydocument.domain.repository.CompanyDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CompanyDocumentRepositoryAdapter implements CompanyDocumentRepository {

    private final SpringDataCompanyDocumentRepository springDataRepository;

    @Override
    public CompanyDocument save(CompanyDocument companyDocument) {
        // saveAndFlush — 제약 위반을 커밋이 아니라 쓰기 시점에 발생시켜 서비스가 변환할 수 있게 한다.
        return CompanyDocumentPersistenceMapper.toDomain(
                springDataRepository.saveAndFlush(CompanyDocumentPersistenceMapper.toEntity(companyDocument)));
    }

    @Override
    public Optional<CompanyDocument> findById(Long companyDocumentId) {
        return springDataRepository.findById(companyDocumentId)
                .map(CompanyDocumentPersistenceMapper::toDomain);
    }
}
