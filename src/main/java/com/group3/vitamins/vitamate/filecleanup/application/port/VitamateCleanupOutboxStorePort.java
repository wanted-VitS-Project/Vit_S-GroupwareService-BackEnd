package com.group3.vitamins.vitamate.filecleanup.application.port;

import com.group3.vitamins.vitamate.filecleanup.application.model.ClaimedVitamateCleanupOutbox;

import java.time.LocalDateTime;
import java.util.List;

public interface VitamateCleanupOutboxStorePort {

    List<ClaimedVitamateCleanupOutbox> claimPublishable(
            String lockOwner,
            int batchSize,
            LocalDateTime now,
            LocalDateTime lockExpiresAt
    );

    void markPublished(
            Long outboxId,
            String lockOwner,
            LocalDateTime publishedAt
    );

    void markPublishFailed(
            Long outboxId,
            String lockOwner,
            String errorMessage,
            LocalDateTime nextAvailableAt
    );
}