package com.group3.vitamins.bidding.bidreview.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record BidReviewDocument(
        Long reviewDocumentId,
        Long reviewId,
        BidReviewDocumentRole documentRole,
        Long bidAttachmentId,
        Long referenceFileId,
        String fileName,
        BidReviewDocumentStatus processingStatus,
        String temporaryStorageKey,
        Long fileSize,
        String mimeType,
        String processingErrorMessage,
        Long promotedFileId,
        Long promotedFileVersionId,
        LocalDateTime promotedAt,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    // 선택한 공고 첨부를 다운로드 대기 상태로 생성합니다.
    public static BidReviewDocument createBidAttachment(
            Long bidAttachmentId,
            String fileName,
            LocalDateTime now
    ) {
        Objects.requireNonNull(bidAttachmentId, "공고 첨부 ID는 필수입니다.");

        return create(
                BidReviewDocumentRole.BID_ATTACHMENT,
                bidAttachmentId,
                null,
                fileName,
                now
        );
    }

    // 선택한 사내 기준자료 스냅샷을 생성합니다.
    public static BidReviewDocument createInternalReference(
            Long referenceFileId,
            String fileName,
            LocalDateTime now
    ) {
        Objects.requireNonNull(referenceFileId, "입찰 기준자료 ID는 필수입니다.");

        return create(
                BidReviewDocumentRole.INTERNAL_REFERENCE,
                null,
                referenceFileId,
                fileName,
                now
        );
    }

    private static BidReviewDocument create(
            BidReviewDocumentRole role,
            Long bidAttachmentId,
            Long referenceFileId,
            String fileName,
            LocalDateTime now
    ) {
        Objects.requireNonNull(role, "문서 역할은 필수입니다.");
        Objects.requireNonNull(fileName, "파일명은 필수입니다.");
        Objects.requireNonNull(now, "생성 시각은 필수입니다.");

        if (fileName.isBlank() || fileName.length() > 500) {
            throw new IllegalArgumentException("파일명이 올바르지 않습니다.");
        }

        return new BidReviewDocument(
                null,
                null,
                role,
                bidAttachmentId,
                referenceFileId,
                fileName,
                BidReviewDocumentStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    // Worker가 내려받은 공고 첨부의 임시 저장 정보를 반영합니다.
    public BidReviewDocument ready(
            String storageKey,
            long fileSize,
            String mimeType,
            LocalDateTime now
    ) {
        if (documentRole != BidReviewDocumentRole.BID_ATTACHMENT) {
            throw new IllegalStateException("공고 첨부만 임시 저장 상태로 전환할 수 있습니다.");
        }
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("임시 저장소 키는 필수입니다.");
        }
        if (fileSize < 1 || fileSize > 52_428_800L) {
            throw new IllegalArgumentException("파일 크기가 허용 범위를 벗어났습니다.");
        }

        return copy(
                BidReviewDocumentStatus.READY,
                storageKey,
                fileSize,
                mimeType,
                null,
                promotedFileId,
                promotedFileVersionId,
                promotedAt,
                deletedAt,
                now
        );
    }

    // 문서 처리 실패 상태와 안전한 오류 메시지를 저장합니다.
    public BidReviewDocument fail(String errorMessage, LocalDateTime now) {
        Objects.requireNonNull(errorMessage, "실패 메시지는 필수입니다.");

        if (errorMessage.isBlank() || errorMessage.length() > 500) {
            throw new IllegalArgumentException("실패 메시지가 올바르지 않습니다.");
        }

        return copy(
                BidReviewDocumentStatus.FAILED,
                temporaryStorageKey,
                fileSize,
                mimeType,
                errorMessage,
                promotedFileId,
                promotedFileVersionId,
                promotedAt,
                deletedAt,
                now
        );
    }

    private BidReviewDocument copy(
            BidReviewDocumentStatus status,
            String storageKey,
            Long fileSize,
            String mimeType,
            String errorMessage,
            Long promotedFileId,
            Long promotedFileVersionId,
            LocalDateTime promotedAt,
            LocalDateTime deletedAt,
            LocalDateTime updatedAt
    ) {
        return new BidReviewDocument(
                reviewDocumentId,
                reviewId,
                documentRole,
                bidAttachmentId,
                referenceFileId,
                fileName,
                status,
                storageKey,
                fileSize,
                mimeType,
                errorMessage,
                promotedFileId,
                promotedFileVersionId,
                promotedAt,
                deletedAt,
                createdAt,
                updatedAt
        );
    }
}