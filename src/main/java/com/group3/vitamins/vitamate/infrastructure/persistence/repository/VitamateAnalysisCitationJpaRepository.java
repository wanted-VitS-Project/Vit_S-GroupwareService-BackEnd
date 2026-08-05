package com.group3.vitamins.vitamate.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.infrastructure.persistence.entity.VitamateAnalysisCitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

// 비타메이트 분석 citation을 저장하는 JPA Repository
public interface VitamateAnalysisCitationJpaRepository extends JpaRepository<VitamateAnalysisCitationEntity, Long> {
}
