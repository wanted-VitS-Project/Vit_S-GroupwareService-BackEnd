package com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.entity.CollectionSourceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataCollectionSourceRepository
        extends JpaRepository<CollectionSourceJpaEntity, Long> {

    // 논리 삭제되지 않은 공용 수집처를 코드로 조회합니다.
    Optional<CollectionSourceJpaEntity> findBySourceCodeAndDeletedAtIsNull(
            String sourceCode
    );
}