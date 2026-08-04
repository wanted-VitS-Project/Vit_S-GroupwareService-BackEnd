package com.group3.vitamins.vitamate.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 비타메이트 분석 요청에 선택된 파일 버전을 보관하는 JPA 엔티티
@Getter
@Entity
@Table(
        name = "vitamate_analysis_document",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vitamate_analysis_document",
                        columnNames = {"vitamate_analysis_id", "file_version_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VitamateAnalysisDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vitamate_analysis_document_id")
    private Long id;

    @Column(name = "vitamate_analysis_id", nullable = false)
    private Long analysisId;

    @Column(name = "file_version_id", nullable = false)
    private Long fileVersionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static VitamateAnalysisDocumentEntity of(Long analysisId, Long fileVersionId) {
        VitamateAnalysisDocumentEntity entity = new VitamateAnalysisDocumentEntity();
        entity.analysisId = analysisId;
        entity.fileVersionId = fileVersionId;
        entity.createdAt = LocalDateTime.now();
        return entity;
    }
}
