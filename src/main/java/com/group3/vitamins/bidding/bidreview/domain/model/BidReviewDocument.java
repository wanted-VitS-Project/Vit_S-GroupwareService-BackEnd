package com.group3.vitamins.bidding.bidreview.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record BidReviewDocument(
        Long reviewDocumentId,
        Long reviewId,
        BidReviewDocumentRole documentRole,
        Long bidAttachmentId,
        Long referenceFileId,
        Long companyDocumentVersionId,
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
                null,
                fileName,
                BidReviewDocumentStatus.PENDING,
                now
        );
    }

    // 선택한 사내 기준자료(bid_reference_file) 스냅샷을 생성합니다. 업로드·인덱싱이 끝난 자료만 검토에
    // 선택할 수 있으므로(BidReviewReferenceFilePort.ReferenceFileSnapshot.isReady() 검증),
    // 생성 시점에 바로 READY로 저장한다 — BID_ATTACHMENT처럼 Worker 다운로드를 기다리지 않는다.
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
                null,
                fileName,
                BidReviewDocumentStatus.READY,
                now
        );
    }

    // 선택한 사내 문서함(company_document_version) 스냅샷을 생성합니다. INTERNAL_REFERENCE와 동일하게
    // 세션 접근 검증이 끝난 완료 버전만 선택되므로 바로 READY로 저장한다.
    public static BidReviewDocument createCompanyDocumentReference(
            Long companyDocumentVersionId,
            String fileName,
            LocalDateTime now
    ) {
        Objects.requireNonNull(companyDocumentVersionId, "사내 문서함 참조 버전 ID는 필수입니다.");

        return create(
                BidReviewDocumentRole.COMPANY_DOCUMENT_REFERENCE,
                null,
                null,
                companyDocumentVersionId,
                fileName,
                BidReviewDocumentStatus.READY,
                now
        );
    }

    private static BidReviewDocument create(
            BidReviewDocumentRole role,
            Long bidAttachmentId,
            Long referenceFileId,
            Long companyDocumentVersionId,
            String fileName,
            BidReviewDocumentStatus initialStatus,
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
                companyDocumentVersionId,
                fileName,
                initialStatus,
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
                companyDocumentVersionId,
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

    // 파일 도메인 정식 file/file_version으로 귀속(승격) 완료를 반영합니다. 다운로드가 끝난 공고 첨부만
    // 귀속 대상입니다 - chk_bid_review_document_promotion 제약과 대응된다.
    public BidReviewDocument promote(Long fileId, Long fileVersionId, LocalDateTime now) {
        Objects.requireNonNull(fileId, "귀속된 파일 ID는 필수입니다.");
        Objects.requireNonNull(fileVersionId, "귀속된 파일 버전 ID는 필수입니다.");
        Objects.requireNonNull(now, "귀속 완료 시각은 필수입니다.");

        if (documentRole != BidReviewDocumentRole.BID_ATTACHMENT) {
            throw new IllegalStateException("공고 첨부만 프로젝트 파일로 귀속할 수 있습니다.");
        }
        if (processingStatus != BidReviewDocumentStatus.READY) {
            throw new IllegalStateException("다운로드가 완료된 문서만 귀속할 수 있습니다.");
        }

        return copy(
                BidReviewDocumentStatus.PROMOTED,
                temporaryStorageKey,
                fileSize,
                mimeType,
                processingErrorMessage,
                fileId,
                fileVersionId,
                now,
                deletedAt,
                now
        );
    }

    // 임시 저장소 객체 삭제 완료를 반영합니다. 공고 첨부만 정리 대상입니다.
    public BidReviewDocument cleanup(LocalDateTime now) {
        Objects.requireNonNull(now, "정리 완료 시각은 필수입니다.");
        if (documentRole != BidReviewDocumentRole.BID_ATTACHMENT) {
            throw new IllegalStateException("공고 첨부만 정리 대상입니다.");
        }

        return copy(
                BidReviewDocumentStatus.DELETED,
                temporaryStorageKey,
                fileSize,
                mimeType,
                processingErrorMessage,
                promotedFileId,
                promotedFileVersionId,
                promotedAt,
                now,
                now
        );
    }
}