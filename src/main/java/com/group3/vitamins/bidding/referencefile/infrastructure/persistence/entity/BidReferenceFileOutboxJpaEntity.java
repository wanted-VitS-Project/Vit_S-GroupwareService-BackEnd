package com.group3.vitamins.bidding.referencefile.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "bid_reference_file_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidReferenceFileOutboxJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_reference_file_outbox_id")
    private Long outboxId;

    @Column(name = "event_id", nullable = false, length = 36, columnDefinition = "CHAR(36)", updatable = false)
    private String eventId;

    @Column(name = "bid_reference_file_id", nullable = false, updatable = false)
    private Long referenceFileId;

    @Column(name = "attempt_id", nullable = false, length = 36, columnDefinition = "CHAR(36)", updatable = false)
    private String attemptId;

    @Column(name = "event_type", nullable = false, length = 50, updatable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "json", updatable = false)
    private JsonNode payload;

    @Column(name = "publish_status", nullable = false, length = 20)
    private String publishStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static BidReferenceFileOutboxJpaEntity pending(
            String eventId,
            Long referenceFileId,
            String attemptId,
            String eventType,
            JsonNode payload,
            LocalDateTime now
    ) {
        BidReferenceFileOutboxJpaEntity entity = new BidReferenceFileOutboxJpaEntity();
        entity.eventId = eventId;
        entity.referenceFileId = referenceFileId;
        entity.attemptId = attemptId;
        entity.eventType = eventType;
        entity.payload = payload;
        entity.publishStatus = "PENDING";
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }
}