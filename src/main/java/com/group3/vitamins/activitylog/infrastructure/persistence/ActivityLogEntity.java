package com.group3.vitamins.activitylog.infrastructure.persistence;

import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_log_id")
    private Long activityLogId;

    @Convert(converter = ActivityLogActionConverter.class)
    @Column(name = "act", nullable = false,
            columnDefinition = "enum('create','delete','modify')")
    private ActivityLogAction act;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "resource_name", length = 255)
    private String resourceName;

    @Column(name = "field", length = 100)
    private String field;

    @Column(name = "before_value", columnDefinition = "text")
    private String beforeValue;

    @Column(name = "after_value", columnDefinition = "text")
    private String afterValue;

    @Column(name = "block_id", nullable = false)
    private Long blockId;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ActivityLogEntity record(
            ActivityLogAction act,
            Long blockId,
            Long resourceId,
            String resourceName,
            String field,
            String beforeValue,
            String afterValue,
            String userId
    ) {
        ActivityLogEntity log = new ActivityLogEntity();
        log.act = act;
        log.blockId = blockId;
        log.resourceId = resourceId;
        log.resourceName = resourceName;
        log.field = field;
        log.beforeValue = beforeValue;
        log.afterValue = afterValue;
        log.userId = userId;
        return log;
    }
}
