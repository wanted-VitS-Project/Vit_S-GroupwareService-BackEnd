package com.group3.vitamins.vitamate.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.infrastructure.persistence.entity.VitamateAnalysisDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

// 비타메이트 분석 대상 문서 엔티티를 저장하는 JPA Repository
public interface VitamateAnalysisDocumentJpaRepository extends JpaRepository<VitamateAnalysisDocumentEntity, Long> {

    // 분석 요청에 연결된 파일 버전 문서들을 조회한다.
    List<VitamateAnalysisDocumentEntity> findByAnalysisIdAndFileVersionIdInAndDeletedAtIsNull(
            Long analysisId,
            Collection<Long> fileVersionIds
    );
}
