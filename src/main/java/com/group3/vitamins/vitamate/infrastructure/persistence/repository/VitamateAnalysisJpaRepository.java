package com.group3.vitamins.vitamate.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.infrastructure.persistence.entity.VitamateAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 비타메이트 분석 요청 엔티티를 저장하고 멱등성 키로 조회하는 JPA Repository
public interface VitamateAnalysisJpaRepository extends JpaRepository<VitamateAnalysisEntity, Long> {

    // 비타메이트 블록, 요청자, 멱등성 키 조합으로 기존 분석 요청을 찾는다.
    Optional<VitamateAnalysisEntity> findByVitamateBlockIdAndRequestedByAndIdempotencyKey(
            Long vitamateBlockId,
            String requestedBy,
            String idempotencyKey
    );
}
