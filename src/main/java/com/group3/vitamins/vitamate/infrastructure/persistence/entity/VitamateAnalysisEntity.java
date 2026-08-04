package com.group3.vitamins.vitamate.infrastructure.persistence.entity;

import com.group3.vitamins.vitamate.domain.model.AnalysisStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 비타메이트 분석 요청과 처리 결과를 보관하는 JPA 엔티티
@Getter
@Entity
@Table(
        name = "vitamate_analysis",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vitamate_analysis_idempotency",
                        columnNames = {"vitamate_block_id", "requested_by", "idempotency_key"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VitamateAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vitamate_analysis_id")
    private Long id;

    @Column(name = "vitamate_block_id", nullable = false)
    private Long vitamateBlockId;

    @Column(name = "requested_by", nullable = false, length = 20)
    private String requestedBy;

    @Lob
    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Lob
    @Column(name = "result", columnDefinition = "LONGTEXT")
    private String result;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 20)
    private AnalysisStatus analysisStatus;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, columnDefinition = "CHAR(64)")
    private String requestHash;

    @Column(name = "processing_attempt_id", length = 36)
    private String processingAttemptId;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "lease_expires_at")
    private LocalDateTime leaseExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static VitamateAnalysisEntity pending(
            Long vitamateBlockId,
            String requestedBy,
            String idempotencyKey,
            String requestHash,
            String prompt,
            LocalDateTime requestedAt
    ) {
        VitamateAnalysisEntity entity = new VitamateAnalysisEntity();
        entity.vitamateBlockId = vitamateBlockId;
        entity.requestedBy = requestedBy;
        entity.idempotencyKey = idempotencyKey;
        entity.requestHash = requestHash;
        entity.prompt = prompt;
        entity.analysisStatus = AnalysisStatus.PENDING;
        entity.createdAt = requestedAt;
        entity.updatedAt = requestedAt;
        return entity;
    }
}
