package com.group3.vitamins.bidding.collectionrun.application.port;

import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;

import java.util.Optional;

public interface CollectionRunConditionPort {

    // 현재 회사가 소유한 수집 조건을 실행 생성 동안 잠금 조회합니다.
    Optional<CollectionCondition> findOwnedConditionForUpdate(
            Long conditionId,
            Long companyId
    );
}