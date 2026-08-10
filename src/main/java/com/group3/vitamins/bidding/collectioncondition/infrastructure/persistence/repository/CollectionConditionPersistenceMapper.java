package com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.entity.CollectionConditionJpaEntity;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.entity.CollectionSourceJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CollectionConditionPersistenceMapper {

    private final CollectionConditionParamsJsonMapper paramsJsonMapper;

    // 수집 조건 JPA Entity를 회사별 수집 조건 도메인 모델로 복원합니다.
    public CollectionCondition toDomain(
            CollectionConditionJpaEntity entity
    ) {
        CollectionConditionParamsJsonMapper.ParsedParams parsedParams =
                paramsJsonMapper.fromJson(entity.getParams());

        return CollectionCondition.restore(
                entity.getCrawlConditionId(),
                entity.getCompanyId(),
                entity.getCrawlSource().getSourceCode(),
                entity.getConditionName(),
                parsedParams.noticeTypes(),
                parsedParams.filters(),
                entity.isEnabled(),
                entity.getLastSuccessAt(),
                entity.getLastCollectedCount(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }

    // 도메인 수집 조건을 저장 가능한 JPA Entity로 변환합니다.
    public CollectionConditionJpaEntity toEntity(
            CollectionCondition condition,
            CollectionSourceJpaEntity sourceEntity
    ) {
        JsonNode params = paramsJsonMapper.toJson(
                condition.getNoticeTypes(),
                condition.getFilters()
        );

        return new CollectionConditionJpaEntity(
                condition.getConditionId(),
                condition.getCompanyId(),
                sourceEntity,
                condition.getConditionName(),
                params,
                condition.isActive(),
                condition.getLastSuccessAt(),
                condition.getLastCollectedCount(),
                condition.getCreatedBy(),
                condition.getCreatedAt(),
                condition.getUpdatedAt(),
                condition.getDeletedAt()
        );
    }
}