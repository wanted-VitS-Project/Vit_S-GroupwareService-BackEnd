package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity;

import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocument;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocumentRole;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocumentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "bid_review_document")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidReviewDocumentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_review_document_id")
    private Long reviewDocumentId;

    @Column(name = "bid_review_id", nullable = false)
    private Long reviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_role", nullable = false, length = 30)
    private BidReviewDocumentRole documentRole;

    @Column(name = "bid_notice_attachment_id")
    private Long bidAttachmentId;

    @Column(name = "bid_reference_file_id")
    private Long referenceFileId;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private BidReviewDocumentStatus processingStatus;

    @Column(name = "temporary_storage_key", length = 1000)
    private String temporaryStorageKey;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "processing_error_message", length = 500)
    private String processingErrorMessage;

    @Column(name = "promoted_file_id")
    private Long promotedFileId;

    @Column(name = "promoted_file_version_id")
    private Long promotedFileVersionId;

    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static BidReviewDocumentJpaEntity from(
            Long reviewId,
            BidReviewDocument document
    ) {
        BidReviewDocumentJpaEntity entity =
                new BidReviewDocumentJpaEntity();
        entity.reviewDocumentId = document.reviewDocumentId();
        entity.reviewId = reviewId;
        entity.apply(document);
        return entity;
    }

    public void apply(BidReviewDocument document) {
        this.documentRole = document.documentRole();
        this.bidAttachmentId = document.bidAttachmentId();
        this.referenceFileId = document.referenceFileId();
        this.fileName = document.fileName();
        this.processingStatus = document.processingStatus();
        this.temporaryStorageKey = document.temporaryStorageKey();
        this.fileSize = document.fileSize();
        this.mimeType = document.mimeType();
        this.processingErrorMessage = document.processingErrorMessage();
        this.promotedFileId = document.promotedFileId();
        this.promotedFileVersionId = document.promotedFileVersionId();
        this.promotedAt = document.promotedAt();
        this.deletedAt = document.deletedAt();
        this.createdAt = document.createdAt();
        this.updatedAt = document.updatedAt();
    }

    public BidReviewDocument toDomain() {
        return new BidReviewDocument(
                reviewDocumentId,
                reviewId,
                documentRole,
                bidAttachmentId,
                referenceFileId,
                fileName,
                processingStatus,
                temporaryStorageKey,
                fileSize,
                mimeType,
                processingErrorMessage,
                promotedFileId,
                promotedFileVersionId,
                promotedAt,
                deletedAt,
                createdAt,
                updatedAt
        );
    }
}
