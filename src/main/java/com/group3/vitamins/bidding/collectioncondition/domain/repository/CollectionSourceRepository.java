package com.group3.vitamins.bidding.collectioncondition.domain.repository;

import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionSource;

import java.util.Optional;

public interface CollectionSourceRepository {

    // 활성 여부와 관계없이 논리 삭제되지 않은 수집처를 코드로 조회합니다.
    Optional<CollectionSource> findNotDeletedByCode(String sourceCode);
}