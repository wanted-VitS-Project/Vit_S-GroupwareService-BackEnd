package com.group3.vitamins.file.infrastructure.persistence;

import com.group3.vitamins.file.domain.model.File;

/** {@link File} 도메인 ↔ {@link FileJpaEntity} 변환. */
public final class FilePersistenceMapper {

    private FilePersistenceMapper() {
    }

    public static File toDomain(FileJpaEntity entity) {
        return File.restore(
                entity.getFileId(),
                entity.getProjectId(),
                entity.getName(),
                entity.getCreatedBy(),
                entity.getDeletedAt()
        );
    }

    public static FileJpaEntity toEntity(File domain) {
        return new FileJpaEntity(
                domain.getFileId(),
                domain.getProjectId(),
                domain.getName(),
                domain.getCreatedBy(),
                domain.getDeletedAt()
        );
    }
}
