package com.group3.vitamins.bidding.referencefile.infrastructure.persistence.entity;

import com.group3.vitamins.bidding.referencefile.domain.model.BidReferenceFile;
import com.group3.vitamins.bidding.referencefile.domain.model.ReferenceFileIndexStatus;
import com.group3.vitamins.bidding.referencefile.domain.model.ReferenceFileUploadStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "bid_reference_file")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidReferenceFileJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_reference_file_id")
    private Long referenceFileId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private Long companyId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "extension", nullable = false, length = 20)
    private String extension;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 1000, updatable = false)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 20)
    private ReferenceFileUploadStatus uploadStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "index_status", nullable = false, length = 20)
    private ReferenceFileIndexStatus indexStatus;

    @Column(name = "index_attempt_id", length = 36, columnDefinition = "CHAR(36)")
    private String indexAttemptId;

    @Column(name = "index_retry_count", nullable = false)
    private int indexRetryCount;

    @Column(name = "index_error_message", length = 500)
    private String indexErrorMessage;

    @Column(name = "upload_expires_at")
    private LocalDateTime uploadExpiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

    @Column(name = "created_by", nullable = false, length = 20, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static BidReferenceFileJpaEntity from(BidReferenceFile referenceFile) {
        BidReferenceFileJpaEntity entity = new BidReferenceFileJpaEntity();
        entity.referenceFileId = referenceFile.referenceFileId();
        entity.apply(referenceFile);
        return entity;
    }

    public void apply(BidReferenceFile referenceFile) {
        this.companyId = referenceFile.companyId();
        this.fileName = referenceFile.fileName();
        this.extension = referenceFile.extension();
        this.mimeType = referenceFile.mimeType();
        this.sizeBytes = referenceFile.sizeBytes();
        this.storageKey = referenceFile.storageKey();
        this.uploadStatus = referenceFile.uploadStatus();
        this.indexStatus = referenceFile.indexStatus();
        this.indexAttemptId = referenceFile.indexAttemptId();
        this.indexRetryCount = referenceFile.indexRetryCount();
        this.indexErrorMessage = referenceFile.indexErrorMessage();
        this.uploadExpiresAt = referenceFile.uploadExpiresAt();
        this.completedAt = referenceFile.completedAt();
        this.indexedAt = referenceFile.indexedAt();
        this.createdBy = referenceFile.createdBy();
        this.createdAt = referenceFile.createdAt();
        this.updatedAt = referenceFile.updatedAt();
        this.deletedAt = referenceFile.deletedAt();
    }

    public BidReferenceFile toDomain() {
        return new BidReferenceFile(
                referenceFileId, companyId, fileName, extension, mimeType, sizeBytes, storageKey,
                uploadStatus, indexStatus, indexAttemptId, indexRetryCount, indexErrorMessage,
                uploadExpiresAt, completedAt, indexedAt, createdBy, createdAt, updatedAt, deletedAt
        );
    }
}