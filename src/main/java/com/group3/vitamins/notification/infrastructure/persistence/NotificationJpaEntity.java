package com.group3.vitamins.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "notification")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    /** 이동 대상 도메인 유형. {@code targetId} 와 함께 있거나 함께 없어야 한다(ck_notification_target) */
    @Column(name = "target_type", length = 50)
    private String targetType;

    /** 이동 대상 PK. 값에 따라 가리키는 테이블이 달라지는 다형성 참조라 FK 가 없다 */
    @Column(name = "target_id")
    private Long targetId;

    @Convert(converter = TargetContextConverter.class)
    @Column(name = "target_context", columnDefinition = "json")
    private Map<String, Object> targetContext;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
