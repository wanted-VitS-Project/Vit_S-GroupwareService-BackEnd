package com.group3.vitamins.bidding.collectioncondition.domain.repository;

import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;

import java.util.List;
import java.util.Optional;

public interface CollectionConditionRepository {

    // 해당 회사가 소유한 논리 삭제되지 않은 수집 조건만 조회합니다.
    List<CollectionCondition> findAllNotDeleted(Long companyId);

    // 해당 회사가 소유한 수집 조건만 ID로 조회합니다.
    Optional<CollectionCondition> findNotDeletedById(
            Long conditionId,
            Long companyId
    );

    // 회사 소유 정보가 포함된 수집 조건을 저장합니다.
    CollectionCondition save(CollectionCondition condition);
}