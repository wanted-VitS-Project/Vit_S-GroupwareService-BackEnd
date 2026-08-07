package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity.VitamateAnalysisTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

// 분석 요청 템플릿 스냅샷을 저장하는 JPA Repository입니다.
public interface VitamateAnalysisTemplateJpaRepository extends JpaRepository<VitamateAnalysisTemplateEntity, Long> {
}
