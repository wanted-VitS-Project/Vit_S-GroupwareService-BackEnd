package com.group3.vitamins.vitamate.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.infrastructure.persistence.entity.VitamateAnalysisDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

// 비타메이트 분석 대상 문서 엔티티를 저장하는 JPA Repository
public interface VitamateAnalysisDocumentJpaRepository extends JpaRepository<VitamateAnalysisDocumentEntity, Long> {
}
