package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity.CollectionRunJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface SpringDataCollectionRunRepository
        extends JpaRepository<CollectionRunJpaEntity, Long> {

    // 같은 조건에 아직 종료되지 않은 실행이 존재하는지 확인합니다.
    boolean existsByCrawlCondition_CrawlConditionIdAndRunStatusInAndDeletedAtIsNull(
            Long conditionId,
            Collection<CollectionRunStatus> statuses
    );

    // 실행 ID와 조건의 회사 ID가 모두 일치하는 실행만 조회합니다.
    @EntityGraph(attributePaths = "crawlCondition")
    Optional<CollectionRunJpaEntity>
    findByCrawlRunIdAndCrawlCondition_CompanyIdAndDeletedAtIsNull(
            Long runId,
            Long companyId
    );
}
