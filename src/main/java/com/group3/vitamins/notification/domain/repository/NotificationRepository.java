package com.group3.vitamins.notification.domain.repository;

import com.group3.vitamins.notification.domain.model.Notification;
import com.group3.vitamins.notification.domain.model.NotificationPage;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository {

    /** 새로 만들거나 변경된 알림을 저장한다. */
    Notification save(Notification notification);

    /** 논리 삭제분은 조회하지 않는다(VIW-005). 삭제·이동대상조회의 404 판정에 쓴다. */
    Optional<Notification> findActiveById(Long notificationId);

    /**
     * VIW-001~004 — 본인 알림만, 최신순, category(notification_type 접두어)·isRead 로 필터링한 목록.
     *
     * @param categoryPrefix null 이면 전체 카테고리
     * @param isRead         null 이면 읽음 여부 무관, true/false 면 해당 조건만
     */
    NotificationPage search(String userId, String categoryPrefix, Boolean isRead, int page, int size);

    /** ACT-005 — 본인의 안 읽은 알림 전체를 일괄 읽음 처리하고 처리 건수를 반환한다. */
    int markAllRead(String userId, LocalDateTime now);

    /** RET-001 — {@code createdBefore} 이전에 생성된 알림을 전부(전 사용자 대상) 논리 삭제하고 처리 건수를 반환한다. */
    int deleteCreatedBefore(LocalDateTime createdBefore, LocalDateTime deletedAt);
}
