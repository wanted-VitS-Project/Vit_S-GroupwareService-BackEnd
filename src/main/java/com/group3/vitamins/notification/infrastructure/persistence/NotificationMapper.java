package com.group3.vitamins.notification.infrastructure.persistence;

import com.group3.vitamins.notification.domain.model.Notification;

public class NotificationMapper {

    private NotificationMapper() {
    }

    /** JPA 엔티티를 도메인 객체로 복원한다. */
    public static Notification toDomain(NotificationJpaEntity entity) {
        return Notification.restore(
                entity.getNotificationId(),
                entity.getUserId(),
                entity.getNotificationType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getTargetContext(),
                entity.getReadAt(),
                entity.getDeletedAt(),
                entity.getCreatedAt()
        );
    }

    /** 도메인 객체를 JPA 엔티티로 옮긴다. */
    public static NotificationJpaEntity toEntity(Notification domain) {
        return new NotificationJpaEntity(
                domain.getNotificationId(),
                domain.getUserId(),
                domain.getNotificationType(),
                domain.getTitle(),
                domain.getMessage(),
                domain.getTargetType(),
                domain.getTargetId(),
                domain.getTargetContext(),
                domain.getReadAt(),
                domain.getDeletedAt(),
                domain.getCreatedAt()
        );
    }
}
