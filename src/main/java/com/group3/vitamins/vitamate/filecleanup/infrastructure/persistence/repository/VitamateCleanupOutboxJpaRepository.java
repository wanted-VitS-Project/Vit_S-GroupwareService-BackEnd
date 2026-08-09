package com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.entity.VitamateCleanupOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VitamateCleanupOutboxJpaRepository
        extends JpaRepository<VitamateCleanupOutboxEntity, Long> {

    // 다른 발행자가 점유하지 않은 PENDING Outbox를 잠금 조회합니다.
    @Query(value = """
            SELECT cleanup_outbox_id
            FROM vitamate_cleanup_outbox
            WHERE publish_status = 'PENDING'
              AND available_at <= :now
              AND (
                    lock_expires_at IS NULL
                    OR lock_expires_at < :now
              )
            ORDER BY cleanup_outbox_id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Long> findPublishableIdsForUpdate(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );
}