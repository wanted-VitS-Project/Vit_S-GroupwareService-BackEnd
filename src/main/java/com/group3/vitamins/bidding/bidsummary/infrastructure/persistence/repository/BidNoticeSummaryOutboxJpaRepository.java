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

    // markExhaustedAsFailed가 벌크 네이티브 UPDATE라 어떤 행이 바뀌었는지 알 수 없다 - 같은 조건으로
    // summaryId를 먼저 조회해 둬야 호출부가 bid_notice_summary.summary_status를 함께 전이시킬 수 있다.
    @Query(value = """
            SELECT bid_notice_summary_id
            FROM bid_notice_summary_outbox
            WHERE publish_status = 'PENDING'
              AND publish_attempt_count >= 5
              AND (lock_expires_at IS NULL OR lock_expires_at < :now)
            """, nativeQuery = true)
    List<Long> findExhaustedSummaryIds(@Param("now") LocalDateTime now);

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

    // 고아 복구 스케줄러가 후보 재확인(TOCTOU 방지)에 쓴다 - 후보 조회와 잠금 사이 정상 흐름이
    // 이미 새 outbox를 만들었을 수 있다.
    boolean existsBySummaryIdAndPublishStatus(Long summaryId, String publishStatus);
}