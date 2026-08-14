package com.group3.vitamins.bidding.collectionrun.application.service;

import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunOutboxStorePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
public class CollectionRunOutboxCleanupService {

    private final CollectionRunOutboxStorePort outboxStorePort;
    private final Clock clock;
    private final int retentionDays;

    public CollectionRunOutboxCleanupService(
            CollectionRunOutboxStorePort outboxStorePort,
            Clock clock,
            @Value("${bidding.collection.outbox.retention-days:7}") int retentionDays
    ) {
        if (retentionDays <= 0) {
            throw new IllegalArgumentException(
                    "Outbox 보관 기간은 1일 이상이어야 합니다."
            );
        }
        this.outboxStorePort = outboxStorePort;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    // 발행 완료된 지 보관 기간이 지난 Outbox 행을 정리합니다. FAILED는 장애 이력이라 대상이 아닙니다.
    public int cleanupPublished() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retentionDays);
        return outboxStorePort.deletePublishedBefore(cutoff);
    }
}
