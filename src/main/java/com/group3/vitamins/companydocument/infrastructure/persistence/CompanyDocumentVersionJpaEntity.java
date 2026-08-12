package com.group3.vitamins.companydocument.infrastructure.persistence;

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
 * {@code company_document_version} 테이블 매핑 (append-only 버전 이력).
 *
 * <p>{@code upload_status} 는 문자열로 저장하고 도메인 {@code UploadStatus} enum 과의 변환은
 * {@code CompanyDocumentVersionPersistenceMapper} 가 한다. {@code created_at} 은 DB 기본값이 관리한다.
 * ⚠️ file 과 달리 {@code uploader_name} 이 **nullable** 이다 — ADMIN 은 employee 행이 없어 스냅샷이 빌 수 있다(§6-6).
 */
@Entity
@Table(name = "company_document_version")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyDocumentVersionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_document_version_id")
    private Long companyDocumentVersionId;

    @Column(name = "company_document_id", nullable = false)
    private Long companyDocumentId;

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

    // ⚠️ nullable — ADMIN 업로더는 employee 행이 없어 이름/부서/직책 스냅샷이 빌 수 있다(§6-6).
    @Column(name = "uploader_name", length = 50)
    private String uploaderName;

    @Column(name = "uploader_department", length = 50)
    private String uploaderDepartment;

    @Column(name = "uploader_position", length = 30)
    private String uploaderPosition;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
