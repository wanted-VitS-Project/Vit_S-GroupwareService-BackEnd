package com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.mapper;

import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionSource;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.entity.CollectionSourceJpaEntity;

public final class CollectionSourcePersistenceMapper {

    private CollectionSourcePersistenceMapper() {
    }

    // JPA 수집처 Entity를 공용 수집처 도메인 모델로 변환합니다.
    public static CollectionSource toDomain(
            CollectionSourceJpaEntity entity
    ) {
        return new CollectionSource(
                entity.getCrawlSourceId(),
                entity.getSourceCode(),
                entity.getSourceName(),
                entity.getSourceType(),
                entity.isEnabled()
        );
    }
}