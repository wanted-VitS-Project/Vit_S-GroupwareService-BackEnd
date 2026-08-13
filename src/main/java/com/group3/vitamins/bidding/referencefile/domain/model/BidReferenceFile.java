package com.group3.vitamins.bidding.referencefile.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record BidReferenceFile(
        Long referenceFileId,
        Long companyId,
        String fileName,
        String extension,
        String mimeType,
        long sizeBytes,
        String storageKey,
        ReferenceFileUploadStatus uploadStatus,
        ReferenceFileIndexStatus indexStatus,
        String indexAttemptId,
        int indexRetryCount,
        String indexErrorMessage,
        LocalDateTime uploadExpiresAt,
        LocalDateTime completedAt,
        LocalDateTime indexedAt,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {

    public static BidReferenceFile createUploading(
            Long companyId,
            String fileName,
            String extension,
            String mimeType,
            long sizeBytes,
            String storageKey,
            String createdBy,
            LocalDateTime uploadExpiresAt,
            LocalDateTime now
    ) {
        Objects.requireNonNull(companyId, "회사 ID는 필수입니다.");
        Objects.requireNonNull(fileName, "파일명은 필수입니다.");
        Objects.requireNonNull(storageKey, "저장소 키는 필수입니다.");
        Objects.requireNonNull(createdBy, "등록자는 필수입니다.");

        return new BidReferenceFile(
                null, companyId, fileName, extension, mimeType, sizeBytes, storageKey,
                ReferenceFileUploadStatus.UPLOADING, ReferenceFileIndexStatus.PENDING,
                null, 0, null, uploadExpiresAt, null, null,
                createdBy, now, now, null
        );
    }

    // 업로드 완료를 확인하고 인덱싱 시도 ID를 새로 발급합니다.
    public BidReferenceFile completeUpload(LocalDateTime now) {
        return new BidReferenceFile(
                referenceFileId, companyId, fileName, extension, mimeType, sizeBytes, storageKey,
                ReferenceFileUploadStatus.COMPLETED, ReferenceFileIndexStatus.PENDING,
                UUID.randomUUID().toString(), indexRetryCount, null,
                uploadExpiresAt, now, indexedAt, createdBy, createdAt, now, deletedAt
        );
    }

    public BidReferenceFile markUploadFailed(LocalDateTime now) {
        return new BidReferenceFile(
                referenceFileId, companyId, fileName, extension, mimeType, sizeBytes, storageKey,
                ReferenceFileUploadStatus.FAILED, indexStatus, indexAttemptId, indexRetryCount,
                indexErrorMessage, uploadExpiresAt, completedAt, indexedAt, createdBy, createdAt, now, deletedAt
        );
    }

    public BidReferenceFile delete(LocalDateTime now) {
        return new BidReferenceFile(
                referenceFileId, companyId, fileName, extension, mimeType, sizeBytes, storageKey,
                uploadStatus, indexStatus, indexAttemptId, indexRetryCount, indexErrorMessage,
                uploadExpiresAt, completedAt, indexedAt, createdBy, createdAt, now, now
        );
    }

    public boolean selectable() {
        return uploadStatus == ReferenceFileUploadStatus.COMPLETED
                && indexStatus == ReferenceFileIndexStatus.COMPLETED;
    }
}