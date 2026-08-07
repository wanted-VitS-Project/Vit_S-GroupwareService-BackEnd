package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity;

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

import java.time.LocalDateTime;

// 분석 요청 당시 선택된 검토 템플릿을 스냅샷으로 보관하는 JPA 엔티티입니다.
@Getter
@Entity
@Table(
        name = "vitamate_analysis_template",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vitamate_analysis_template_analysis_category",
                        columnNames = {"vitamate_analysis_id", "category_code"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VitamateAnalysisTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vitamate_analysis_template_id")
    private Long id;

    @Column(name = "vitamate_analysis_id", nullable = false)
    private Long analysisId;

    @Column(name = "review_type", nullable = false, length = 50)
    private String reviewType;

    @Column(name = "category_code", nullable = false, length = 50)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Lob
    @Column(name = "prompt_template", nullable = false, columnDefinition = "TEXT")
    private String promptTemplate;

    @Column(name = "template_version", nullable = false, length = 50)
    private String templateVersion;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 선택된 마스터 템플릿을 분석 요청 단위 스냅샷으로 생성합니다.
    public static VitamateAnalysisTemplateEntity of(
            Long analysisId,
            String reviewType,
            String categoryCode,
            String categoryName,
            String promptTemplate,
            String templateVersion,
            Integer sortOrder,
            LocalDateTime createdAt
    ) {
        VitamateAnalysisTemplateEntity entity = new VitamateAnalysisTemplateEntity();
        entity.analysisId = analysisId;
        entity.reviewType = reviewType;
        entity.categoryCode = categoryCode;
        entity.categoryName = categoryName;
        entity.promptTemplate = promptTemplate;
        entity.templateVersion = templateVersion;
        entity.sortOrder = sortOrder;
        entity.createdAt = createdAt;
        return entity;
    }
}
