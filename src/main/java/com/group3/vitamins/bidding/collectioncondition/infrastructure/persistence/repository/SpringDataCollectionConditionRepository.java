package com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.entity.CollectionConditionJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SpringDataCollectionConditionRepository
        extends JpaRepository<CollectionConditionJpaEntity, Long> {

    // 현재 회사의 논리 삭제되지 않은 조건과 공용 수집처를 함께 조회합니다.
    @EntityGraph(attributePaths = "crawlSource")
    List<CollectionConditionJpaEntity>
    findAllByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long companyId
    );

    // 현재 회사가 소유한 조건 한 건과 공용 수집처를 함께 조회합니다.
    @EntityGraph(attributePaths = "crawlSource")
    Optional<CollectionConditionJpaEntity>
    findByCrawlConditionIdAndCompanyIdAndDeletedAtIsNull(
            Long crawlConditionId,
            Long companyId
    );

    // 같은 조건의 실행 생성 요청을 직렬화하기 위해 조건 행을 잠금 조회합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT condition
        FROM CollectionConditionJpaEntity condition
        JOIN FETCH condition.crawlSource
        WHERE condition.crawlConditionId = :conditionId
          AND condition.companyId = :companyId
          AND condition.deletedAt IS NULL
        """)
    Optional<CollectionConditionJpaEntity> findOwnedConditionForUpdate(
            @Param("conditionId") Long conditionId,
            @Param("companyId") Long companyId
    );
}