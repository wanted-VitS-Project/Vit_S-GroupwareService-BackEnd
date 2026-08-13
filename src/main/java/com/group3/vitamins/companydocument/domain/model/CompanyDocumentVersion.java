package com.group3.vitamins.companydocument.domain.model;

import java.time.LocalDateTime;

/**
 * 사내 문서 버전 도메인 객체 (COMPANY-DOC-V1 · `company_document_version`).
 *
 * <p>append-only 이력이다 — 되돌리기·버전 삭제가 없다. 업로더 정보는 **스냅샷**으로 박혀 이후 소속이 바뀌거나
 * 퇴사해도 당시 값이 남는다(INV-06). 단 사내 문서는 ADMIN 이 올려 employee 행이 없을 수 있어
 * 업로더 이름·부서·직책은 **nullable** 이다(§6-6). `uploadedBy`(사번)만 NOT NULL 로 항상 기록한다.
 *
 * <p>생명주기: {@link UploadStatus#UPLOADING} 으로 생성(§1) → 완료 통보(§2)에서 {@link #complete}
 * 로 {@link UploadStatus#COMPLETED} 전환. 저장소에 객체가 없으면 {@link #fail}.
 * file 의 {@code FileVersion} 을 미러링하되 귀속 멱등키(idempotencyKey)는 없다.
 */
public class CompanyDocumentVersion {

    private final Long versionId;
    private final Long companyDocumentId;
    private final int versionNo;
    private UploadStatus uploadStatus;
    private final String storageKey;
    private final String originalFileName;
    private final String extension;
    private final String mimeType;
    private long sizeBytes;
    private String checksum;
    private Integer pageCount;
    private final String comment;
    private final String uploadedBy;
    private final String uploaderName;
    private final String uploaderDepartment;
    private final String uploaderPosition;
    private LocalDateTime completedAt;
    private LocalDateTime deletedAt;

    private CompanyDocumentVersion(Long versionId, Long companyDocumentId, int versionNo, UploadStatus uploadStatus,
                                   String storageKey, String originalFileName, String extension, String mimeType,
                                   long sizeBytes, String checksum, Integer pageCount, String comment,
                                   String uploadedBy, String uploaderName, String uploaderDepartment,
                                   String uploaderPosition, LocalDateTime completedAt, LocalDateTime deletedAt) {
        this.versionId = versionId;
        this.companyDocumentId = companyDocumentId;
        this.versionNo = versionNo;
        this.uploadStatus = uploadStatus;
        this.storageKey = storageKey;
        this.originalFileName = originalFileName;
        this.extension = extension;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.checksum = checksum;
        this.pageCount = pageCount;
        this.comment = comment;
        this.uploadedBy = uploadedBy;
        this.uploaderName = uploaderName;
        this.uploaderDepartment = uploaderDepartment;
        this.uploaderPosition = uploaderPosition;
        this.completedAt = completedAt;
        this.deletedAt = deletedAt;
    }

    /**
     * 업로드 시작(§1) — UPLOADING 상태의 빈 버전을 만든다. presigned 발급 전제라 저장소 키가 이미 정해진다.
     * 업로더 스냅샷은 완료 통보(§2)가 아니라 시작 시점에 확정한다(file 과 동일). ADMIN 이면 이름/부서/직책이 null 일 수 있다.
     */
    public static CompanyDocumentVersion startUpload(Long companyDocumentId, int versionNo, String storageKey,
                                                     String originalFileName, String extension, String mimeType,
                                                     long sizeBytes, String comment, String uploadedBy,
                                                     String uploaderName, String uploaderDepartment,
                                                     String uploaderPosition) {
        return new CompanyDocumentVersion(null, companyDocumentId, versionNo, UploadStatus.UPLOADING, storageKey,
                originalFileName, extension, mimeType, sizeBytes, null, null, comment,
                uploadedBy, uploaderName, uploaderDepartment, uploaderPosition, null, null);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static CompanyDocumentVersion restore(Long versionId, Long companyDocumentId, int versionNo,
                                                 UploadStatus uploadStatus, String storageKey, String originalFileName,
                                                 String extension, String mimeType, long sizeBytes, String checksum,
                                                 Integer pageCount, String comment, String uploadedBy,
                                                 String uploaderName, String uploaderDepartment, String uploaderPosition,
                                                 LocalDateTime completedAt, LocalDateTime deletedAt) {
        return new CompanyDocumentVersion(versionId, companyDocumentId, versionNo, uploadStatus, storageKey,
                originalFileName, extension, mimeType, sizeBytes, checksum, pageCount, comment, uploadedBy,
                uploaderName, uploaderDepartment, uploaderPosition, completedAt, deletedAt);
    }

    /**
     * 완료 통보(§2) — S3 HEAD 검증 후 COMPLETED 로 확정한다. PDF 페이지 수는 실패해도 null 로 둔다.
     * ⚠️ UPLOADING 에서만 전이한다 — 중복 완료 통보가 값을 덮어쓰거나 FAILED 를 되살리는 것을 막는다.
     */
    public void complete(long verifiedSizeBytes, String checksum, Integer pageCount, LocalDateTime completedAt) {
        requireUploading();
        this.uploadStatus = UploadStatus.COMPLETED;
        this.sizeBytes = verifiedSizeBytes;
        this.checksum = checksum;
        this.pageCount = pageCount;
        this.completedAt = completedAt;
    }

    /** 저장소에 객체가 없거나 검증 실패 시 FAILED 로 전환한다(§2). UPLOADING 에서만 전이한다. */
    public void fail() {
        requireUploading();
        this.uploadStatus = UploadStatus.FAILED;
    }

    /** UPLOADING 이 아니면 상태 전이를 거부한다(이미 종료된 버전 보호). 서비스가 먼저 막으므로 방어선이다. */
    private void requireUploading() {
        if (uploadStatus != UploadStatus.UPLOADING) {
            throw new IllegalStateException("업로드 중(UPLOADING) 버전만 상태를 전이할 수 있습니다: " + uploadStatus);
        }
    }

    public boolean isUploading() {
        return uploadStatus == UploadStatus.UPLOADING;
    }

    public boolean isCompleted() {
        return uploadStatus == UploadStatus.COMPLETED;
    }

    /** PDF 만 미리보기 가능(§9 · previewable = 확장자 기준). */
    public boolean isPreviewable() {
        return "pdf".equalsIgnoreCase(extension);
    }

    public Long getVersionId() {
        return versionId;
    }

    public Long getCompanyDocumentId() {
        return companyDocumentId;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public UploadStatus getUploadStatus() {
        return uploadStatus;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getChecksum() {
        return checksum;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public String getComment() {
        return comment;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public String getUploaderDepartment() {
        return uploaderDepartment;
    }

    public String getUploaderPosition() {
        return uploaderPosition;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
