package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity.CollectionRunOutboxJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CollectionRunOutboxJpaRepository
        extends JpaRepository<CollectionRunOutboxJpaEntity, Long> {

    // 다른 Dispatcher가 점유하지 않은 발행 대기 Outbox를 잠금 조회합니다.
    @Query(value = """
            SELECT crawl_run_outbox_id
            FROM crawl_run_outbox
            WHERE publish_status = 'PENDING'
              AND available_at <= :now
              AND (
                    lock_expires_at IS NULL
                    OR lock_expires_at < :now
              )
            ORDER BY crawl_run_outbox_id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Long> findPublishableIdsForUpdate(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );
}