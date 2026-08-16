package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.entity;

import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// file_index 테이블의 인덱싱 상태를 저장하는 JPA 엔티티
@Getter
@Entity
@Table(name = "file_index")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileIndexEntity {

    @Id
    @Column(name = "file_version_id")
    private Long fileVersionId;

    @Column(name = "index_attempt_id", length = 36)
    private String indexAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "index_status", nullable = false, length = 20)
    private FileIndexStatus indexStatus;

    @Column(name = "index_error_message")
    private String indexErrorMessage;

    // 네이티브 INSERT(upsertStatus)가 이 컬럼을 명시적으로 안 채우고 DB DEFAULT에 맡기므로,
    // Flyway가 꺼진 테스트(H2 ddl-auto)에서도 같은 기본값이 생성되도록 columnDefinition을 둔다.
    @Column(name = "retry_count", nullable = false, columnDefinition = "integer default 0")
    private int retryCount;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "lease_expires_at")
    private LocalDateTime leaseExpiresAt;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // fileVersionId 기준으로 새 인덱싱 상태 row를 만든다.
    public FileIndexEntity(Long fileVersionId, LocalDateTime now) {
        this.fileVersionId = fileVersionId;
        this.indexStatus = FileIndexStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Python callback 상태에 맞춰 인덱싱 상태를 갱신한다.
    public void changeStatus(String indexAttemptId, FileIndexStatus status, String errorMessage, LocalDateTime now) {
        this.indexAttemptId = indexAttemptId;
        this.indexStatus = status;
        this.updatedAt = now;
        this.deletedAt = null;

        if (status == FileIndexStatus.COMPLETED) {
            this.indexErrorMessage = null;
            this.indexedAt = now;
            return;
        }

        if (status == FileIndexStatus.FAILED) {
            this.indexErrorMessage = errorMessage;
            this.indexedAt = null;
            return;
        }

        this.indexErrorMessage = null;
        this.indexedAt = null;
    }
}
