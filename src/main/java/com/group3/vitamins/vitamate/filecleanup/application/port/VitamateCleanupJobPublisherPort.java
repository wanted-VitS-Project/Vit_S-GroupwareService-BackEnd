package com.group3.vitamins.vitamate.filecleanup.application.port;

import com.group3.vitamins.vitamate.filecleanup.application.model.ClaimedVitamateCleanupOutbox;

public interface VitamateCleanupJobPublisherPort {

    // 점유한 ChromaDB 정리 작업을 외부 메시지 큐에 발행합니다.
    void publish(ClaimedVitamateCleanupOutbox outbox);
}