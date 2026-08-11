package com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.entity.CollectionConditionJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
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

    // 여러 서버가 동시에 조회해도 이미 잠긴 조건은 건너뛰고 실행 대상만 점유합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(
            name = "jakarta.persistence.lock.timeout",
            value = "-2"
    ))
    @Query("""
        SELECT condition
        FROM CollectionConditionJpaEntity condition
        WHERE condition.enabled = true
          AND condition.autoCollectionEnabled = true
          AND condition.nextRunAt IS NOT NULL
          AND condition.nextRunAt <= :now
          AND condition.deletedAt IS NULL
        ORDER BY condition.nextRunAt ASC, condition.crawlConditionId ASC
        """)
    List<CollectionConditionJpaEntity> findDueConditionsForUpdate(
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE CollectionConditionJpaEntity condition
        SET condition.nextRunAt = :nextRunAt,
            condition.updatedAt = :updatedAt
        WHERE condition.crawlConditionId = :conditionId
          AND condition.deletedAt IS NULL
        """)
    int advanceSchedule(
            @Param("conditionId") Long conditionId,
            @Param("nextRunAt") LocalDateTime nextRunAt,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE CollectionConditionJpaEntity condition
        SET condition.lastScheduledAt = :scheduledAt,
            condition.nextRunAt = :nextRunAt,
            condition.updatedAt = :updatedAt
        WHERE condition.crawlConditionId = :conditionId
          AND condition.deletedAt IS NULL
        """)
    int recordScheduledRun(
            @Param("conditionId") Long conditionId,
            @Param("scheduledAt") LocalDateTime scheduledAt,
            @Param("nextRunAt") LocalDateTime nextRunAt,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE CollectionConditionJpaEntity condition
        SET condition.lastSuccessAt = :successAt,
            condition.lastCollectedCount = :collectedCount,
            condition.updatedAt = :successAt
        WHERE condition.crawlConditionId = :conditionId
          AND condition.deletedAt IS NULL
        """)
    int recordCollectionSuccess(
            @Param("conditionId") Long conditionId,
            @Param("successAt") LocalDateTime successAt,
            @Param("collectedCount") int collectedCount
    );
}
