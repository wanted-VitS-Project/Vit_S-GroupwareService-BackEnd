package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity;

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
@Table(
        name = "bid_notice_raw",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bid_notice_raw_notice_hash",
                columnNames = {
                        "bid_notice_id",
                        "raw_payload_hash"
                }
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidNoticeRawJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_notice_raw_id")
    private Long bidNoticeRawId;

    @Column(name = "bid_notice_id", nullable = false)
    private Long bidNoticeId;

    @Column(name = "crawl_run_id")
    private Long crawlRunId;

    @Column(name = "source_code", nullable = false, length = 30)
    private String sourceCode;

    @Column(name = "payload_type", nullable = false, length = 20)
    private String payloadType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "raw_payload",
            nullable = false,
            columnDefinition = "JSON",
            updatable = false
    )
    private JsonNode rawPayload;

    @Column(
            name = "raw_payload_hash",
            nullable = false,
            length = 64,
            columnDefinition = "CHAR(64)",
            updatable = false
    )
    private String rawPayloadHash;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 수집된 공고 원문을 새로운 이력으로 생성합니다.
    public static BidNoticeRawJpaEntity create(
            Long bidNoticeId,
            Long crawlRunId,
            String sourceCode,
            JsonNode rawPayload,
            String rawPayloadHash,
            LocalDateTime parsedAt
    ) {
        BidNoticeRawJpaEntity entity = new BidNoticeRawJpaEntity();
        entity.bidNoticeId = bidNoticeId;
        entity.crawlRunId = crawlRunId;
        entity.sourceCode = sourceCode;
        entity.payloadType = "JSON";
        entity.rawPayload = rawPayload;
        entity.rawPayloadHash = rawPayloadHash;
        entity.parsedAt = parsedAt;
        entity.createdAt = parsedAt;
        return entity;
    }
}