package com.group3.vitamins.file.domain.model;

import java.time.LocalDateTime;

/**
 * 파일 버전 도메인 객체 (`.ai/api/file.md` · `file_version`).
 *
 * <p>append-only 이력이다 — 되돌리기·버전 삭제가 없다(VER-007). 업로더 정보는 **스냅샷**으로 박혀
 * 이후 소속이 바뀌거나 퇴사해도 당시 값이 남는다(VER-006). NOT NULL 이라 업로드 시작 시점에 확정한다.
 *
 * <p>생명주기: {@link UploadStatus#UPLOADING} 으로 생성(§1) → 완료 통보(§2)에서 {@link #complete}
 * 로 {@link UploadStatus#COMPLETED} 전환(페이지 수·체크섬·완료시각 확정). 저장소에 객체가 없으면 {@link #fail}.
 */
public class FileVersion {

    private final Long fileVersionId;
    private final Long fileId;
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

    private FileVersion(Long fileVersionId, Long fileId, int versionNo, UploadStatus uploadStatus,
                        String storageKey, String originalFileName, String extension, String mimeType,
                        long sizeBytes, String checksum, Integer pageCount, String comment,
                        String uploadedBy, String uploaderName, String uploaderDepartment,
                        String uploaderPosition, LocalDateTime completedAt, LocalDateTime deletedAt) {
        this.fileVersionId = fileVersionId;
        this.fileId = fileId;
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
     * 업로더 스냅샷은 NOT NULL 이라 이 시점에 확정한다.
     */
    public static FileVersion startUpload(Long fileId, int versionNo, String storageKey,
                                          String originalFileName, String extension, String mimeType,
                                          long sizeBytes, String comment,
                                          String uploadedBy, String uploaderName,
                                          String uploaderDepartment, String uploaderPosition) {
        return new FileVersion(null, fileId, versionNo, UploadStatus.UPLOADING, storageKey,
                originalFileName, extension, mimeType, sizeBytes, null, null, comment,
                uploadedBy, uploaderName, uploaderDepartment, uploaderPosition, null, null);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static FileVersion restore(Long fileVersionId, Long fileId, int versionNo, UploadStatus uploadStatus,
                                      String storageKey, String originalFileName, String extension, String mimeType,
                                      long sizeBytes, String checksum, Integer pageCount, String comment,
                                      String uploadedBy, String uploaderName, String uploaderDepartment,
                                      String uploaderPosition, LocalDateTime completedAt, LocalDateTime deletedAt) {
        return new FileVersion(fileVersionId, fileId, versionNo, uploadStatus, storageKey, originalFileName,
                extension, mimeType, sizeBytes, checksum, pageCount, comment, uploadedBy, uploaderName,
                uploaderDepartment, uploaderPosition, completedAt, deletedAt);
    }

    /** 완료 통보(§2) — S3 HEAD 검증 후 COMPLETED 로 확정한다. PDF 페이지 수는 실패해도 null 로 둔다(VER-008). */
    public void complete(long verifiedSizeBytes, String checksum, Integer pageCount, LocalDateTime completedAt) {
        this.uploadStatus = UploadStatus.COMPLETED;
        this.sizeBytes = verifiedSizeBytes;
        this.checksum = checksum;
        this.pageCount = pageCount;
        this.completedAt = completedAt;
    }

    /** 저장소에 객체가 없거나 검증 실패 시 FAILED 로 전환한다(§2). */
    public void fail() {
        this.uploadStatus = UploadStatus.FAILED;
    }

    public boolean isCompleted() {
        return uploadStatus == UploadStatus.COMPLETED;
    }

    /** PDF 만 미리보기 가능(§10 · previewable = 확장자 기준, 2026-08-06 확정). */
    public boolean isPreviewable() {
        return "pdf".equalsIgnoreCase(extension);
    }

    public Long getFileVersionId() {
        return fileVersionId;
    }

    public Long getFileId() {
        return fileId;
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
