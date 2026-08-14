package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity.CollectionRunOutboxJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CollectionRunOutboxJpaRepository
        extends JpaRepository<CollectionRunOutboxJpaEntity, Long> {

    // 마지막 발행 시도 중 프로세스가 종료된 Outbox를 종료 상태로 정리합니다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE crawl_run_outbox
            SET publish_status = 'FAILED',
                lock_owner = NULL,
                lock_expires_at = NULL,
                last_error_message = 'PUBLISH_RETRY_EXHAUSTED',
                updated_at = :now
            WHERE publish_status = 'PENDING'
              AND publish_attempt_count >= 5
              AND (lock_expires_at IS NULL OR lock_expires_at < :now)
            """, nativeQuery = true)
    int markExhaustedAsFailed(@Param("now") LocalDateTime now);

    // 다른 Dispatcher가 점유하지 않은 발행 대기 Outbox를 잠금 조회합니다.
    @Query(value = """
            SELECT crawl_run_outbox_id
            FROM crawl_run_outbox
            WHERE publish_status = 'PENDING'
              AND publish_attempt_count < 5
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

    // 발행 완료 후 보관 기간이 지난 Outbox 행을 정리합니다. FAILED는 장애 이력이라 대상이 아닙니다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM crawl_run_outbox
            WHERE publish_status = 'PUBLISHED'
              AND published_at < :cutoff
            """, nativeQuery = true)
    int deletePublishedBefore(@Param("cutoff") LocalDateTime cutoff);
}
