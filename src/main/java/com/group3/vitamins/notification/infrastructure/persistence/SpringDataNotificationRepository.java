package com.group3.vitamins.notification.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, Long> {

    /** 삭제되지 않은 알림만 ID 로 찾는다 (삭제·읽음·이동대상조회의 404 판정용). */
    Optional<NotificationJpaEntity> findByNotificationIdAndDeletedAtIsNull(Long notificationId);

    /**
     * VIW-001~004 — 본인 알림, 최신순, category(notification_type 접두어 일치)·isRead 필터.
     * category/isRead 가 null 이면 해당 조건을 건너뛴다.
     */
    @Query("""
            SELECT n FROM NotificationJpaEntity n
            WHERE n.userId = :userId
              AND n.deletedAt IS NULL
              AND (:category IS NULL OR n.notificationType LIKE CONCAT(:category, '%'))
              AND (:isRead IS NULL
                   OR (:isRead = TRUE AND n.readAt IS NOT NULL)
                   OR (:isRead = FALSE AND n.readAt IS NULL))
            ORDER BY n.createdAt DESC, n.notificationId DESC
            """)
    Page<NotificationJpaEntity> search(@Param("userId") String userId,
                                       @Param("category") String category,
                                       @Param("isRead") Boolean isRead,
                                       Pageable pageable);
}
