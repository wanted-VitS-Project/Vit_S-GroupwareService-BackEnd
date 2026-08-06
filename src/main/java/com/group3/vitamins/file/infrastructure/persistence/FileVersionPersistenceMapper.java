package com.group3.vitamins.file.infrastructure.persistence;

import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.file.domain.model.UploadStatus;

/** {@link FileVersion} 도메인 ↔ {@link FileVersionJpaEntity} 변환. upload_status 는 문자열↔enum 변환. */
public final class FileVersionPersistenceMapper {

    private FileVersionPersistenceMapper() {
    }

    public static FileVersion toDomain(FileVersionJpaEntity e) {
        return FileVersion.restore(
                e.getFileVersionId(),
                e.getFileId(),
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

    public static FileVersionJpaEntity toEntity(FileVersion d) {
        return new FileVersionJpaEntity(
                d.getFileVersionId(),
                d.getFileId(),
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
