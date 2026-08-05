package com.group3.vitamins.vitamate.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 비타메이트 분석 결과의 근거 citation을 보관하는 JPA 엔티티
@Getter
@Entity
@Table(
        name = "vitamate_analysis_citation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vitamate_analysis_citation_rank",
                        columnNames = {"vitamate_analysis_id", "rank_order"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VitamateAnalysisCitationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vitamate_analysis_citation_id")
    private Long id;

    @Column(name = "vitamate_analysis_id", nullable = false)
    private Long analysisId;

    @Column(name = "vitamate_analysis_document_id", nullable = false)
    private Long analysisDocumentId;

    @Column(name = "document_chunk_id", nullable = false)
    private Long documentChunkId;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Column(name = "distance_score", precision = 10, scale = 6)
    private BigDecimal distanceScore;

    @Lob
    @Column(name = "excerpt", columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 검증된 citation 값으로 저장 엔티티를 만든다.
    public static VitamateAnalysisCitationEntity of(
            Long analysisId,
            Long analysisDocumentId,
            Long documentChunkId,
            Integer rankOrder,
            BigDecimal distanceScore,
            String excerpt
    ) {
        VitamateAnalysisCitationEntity entity = new VitamateAnalysisCitationEntity();
        entity.analysisId = analysisId;
        entity.analysisDocumentId = analysisDocumentId;
        entity.documentChunkId = documentChunkId;
        entity.rankOrder = rankOrder;
        entity.distanceScore = distanceScore;
        entity.excerpt = excerpt;
        entity.createdAt = LocalDateTime.now();
        return entity;
    }
}
