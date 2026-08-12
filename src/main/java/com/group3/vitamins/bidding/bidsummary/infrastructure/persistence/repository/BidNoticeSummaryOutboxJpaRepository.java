package com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryOutboxJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BidNoticeSummaryOutboxJpaRepository
        extends JpaRepository<BidNoticeSummaryOutboxJpaEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE bid_notice_summary_outbox
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

    // SKIP LOCKED로 다른 서버가 점유한 행은 기다리지 않고 건너뜁니다.
    @Query(value = """
            SELECT bid_notice_summary_outbox_id
            FROM bid_notice_summary_outbox
            WHERE publish_status = 'PENDING'
              AND publish_attempt_count < 5
              AND available_at <= :now
              AND (
                    lock_expires_at IS NULL
                    OR lock_expires_at < :now
              )
            ORDER BY bid_notice_summary_outbox_id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Long> findPublishableIdsForUpdate(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );
}