package com.group3.vitamins.file.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * {@code file_version} 테이블 매핑 (append-only 버전 이력).
 *
 * <p>{@code upload_status} 는 문자열로 저장하고 도메인 {@code UploadStatus} enum 과의 변환은
 * {@code FileVersionPersistenceMapper} 가 한다. {@code created_at} 은 DB 기본값이 관리하므로 매핑하지 않는다.
 */
@Entity
@Table(name = "file_version")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileVersionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_version_id")
    private Long fileVersionId;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "upload_status", nullable = false, length = 20)
    private String uploadStatus;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "extension", nullable = false, length = 20)
    private String extension;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "uploaded_by", nullable = false, length = 20)
    private String uploadedBy;

    @Column(name = "uploader_name", nullable = false, length = 50)
    private String uploaderName;

    @Column(name = "uploader_department", length = 50)
    private String uploaderDepartment;

    @Column(name = "uploader_position", length = 30)
    private String uploaderPosition;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;
}
