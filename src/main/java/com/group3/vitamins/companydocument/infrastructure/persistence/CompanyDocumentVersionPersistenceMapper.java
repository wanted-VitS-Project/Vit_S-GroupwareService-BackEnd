package com.group3.vitamins.companydocument.infrastructure.persistence;

import com.group3.vitamins.companydocument.domain.model.CompanyDocumentVersion;
import com.group3.vitamins.companydocument.domain.model.UploadStatus;

/** {@link CompanyDocumentVersion} 도메인 ↔ {@link CompanyDocumentVersionJpaEntity} 변환. upload_status 는 문자열↔enum. */
public final class CompanyDocumentVersionPersistenceMapper {

    private CompanyDocumentVersionPersistenceMapper() {
    }

    public static CompanyDocumentVersion toDomain(CompanyDocumentVersionJpaEntity e) {
        return CompanyDocumentVersion.restore(
                e.getCompanyDocumentVersionId(),
                e.getCompanyDocumentId(),
                e.getVersionNo(),
                UploadStatus.valueOf(e.getUploadStatus()),
                e.getStorageKey(),
                e.getOriginalFileName(),
                e.getExtension(),
                e.getMimeType(),
                e.getSizeBytes(),
                e.getChecksum(),
                e.getPageCount(),
                e.getComment(),
                e.getUploadedBy(),
                e.getUploaderName(),
                e.getUploaderDepartment(),
                e.getUploaderPosition(),
                e.getCompletedAt(),
                e.getDeletedAt()
        );
    }

    public static CompanyDocumentVersionJpaEntity toEntity(CompanyDocumentVersion d) {
        return new CompanyDocumentVersionJpaEntity(
                d.getVersionId(),
                d.getCompanyDocumentId(),
                d.getVersionNo(),
                d.getUploadStatus().name(),
                d.getStorageKey(),
                d.getOriginalFileName(),
                d.getExtension(),
                d.getMimeType(),
                d.getSizeBytes(),
                d.getChecksum(),
                d.getPageCount(),
                d.getComment(),
                d.getUploadedBy(),
                d.getUploaderName(),
                d.getUploaderDepartment(),
                d.getUploaderPosition(),
                d.getCompletedAt(),
                d.getDeletedAt()
        );
    }
}
