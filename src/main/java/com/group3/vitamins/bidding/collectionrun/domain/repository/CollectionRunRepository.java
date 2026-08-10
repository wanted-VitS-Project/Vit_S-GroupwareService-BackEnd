package com.group3.vitamins.bidding.collectionrun.domain.repository;

import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRun;

import java.util.Optional;

public interface CollectionRunRepository {

    // 같은 조건에 종료되지 않은 실행이 존재하는지 확인합니다.
    boolean existsActiveByConditionId(Long conditionId);

    // 수집 실행과 요청자 정보를 저장합니다.
    CollectionRun save(CollectionRun collectionRun);

    // 수집 조건의 회사 소유권까지 확인하여 실행 결과를 조회합니다.
    Optional<CollectionRun> findByIdAndCompanyId(Long runId, Long companyId);
}
