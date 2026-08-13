package com.group3.vitamins.companydocument.infrastructure.persistence;

import com.group3.vitamins.companydocument.domain.model.CompanyDocument;
import com.group3.vitamins.companydocument.domain.model.DocumentCategory;

/** {@link CompanyDocument} 도메인 ↔ {@link CompanyDocumentJpaEntity} 변환. category 는 문자열↔enum 변환. */
public final class CompanyDocumentPersistenceMapper {

    private CompanyDocumentPersistenceMapper() {
    }

    public static CompanyDocument toDomain(CompanyDocumentJpaEntity e) {
        return CompanyDocument.restore(
                e.getCompanyDocumentId(),
                e.getCompanyId(),
                DocumentCategory.valueOf(e.getCategory()),
                e.getName(),
                e.getCreatedBy(),
                e.getDeletedAt()
        );
    }

    public static CompanyDocumentJpaEntity toEntity(CompanyDocument d) {
        return new CompanyDocumentJpaEntity(
                d.getCompanyDocumentId(),
                d.getCompanyId(),
                d.getCategory().name(),
                d.getName(),
                d.getCreatedBy(),
                d.getDeletedAt()
        );
    }
}
