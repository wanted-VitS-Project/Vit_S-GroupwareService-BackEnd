package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunOutbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("CollectionRunOutboxJpaEntity 상태 전이")
class CollectionRunOutboxJpaEntityTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 10, 16, 30);

    @Test
    @DisplayName("다섯 번째 발행 실패 후 FAILED로 종료한다")
    void marksFailedAfterFifthPublishAttempt() {
        CollectionRunOutboxJpaEntity entity = pending();

        for (int attempt = 1; attempt <= 5; attempt++) {
            String owner = "dispatcher-" + attempt;
            entity.claim(owner, NOW.plusMinutes(5), NOW);
            assertThat(entity.markPublishFailed(
                    owner,
                    "REDIS_PUBLISH_FAILED",
                    NOW.plusMinutes(1),
                    NOW
            )).isTrue();
        }

        assertThat(entity.getPublishStatus())
                .isEqualTo(CollectionRunOutbox.PublishStatus.FAILED);
        assertThat(entity.getPublishAttemptCount()).isEqualTo(5);
        assertThat(entity.getLastErrorMessage())
                .isEqualTo("REDIS_PUBLISH_FAILED");
    }

    @Test
    @DisplayName("다른 Dispatcher의 상태 변경 요청을 거부한다")
    void rejectsTransitionByDifferentOwner() {
        CollectionRunOutboxJpaEntity entity = pending();
        entity.claim("owner-a", NOW.plusMinutes(5), NOW);

        assertThat(entity.markPublished("owner-b", NOW)).isFalse();
        assertThat(entity.getPublishStatus())
                .isEqualTo(CollectionRunOutbox.PublishStatus.PENDING);
    }

    @Test
    @DisplayName("손상된 payload는 FAILED로 종료한다")
    void marksInvalidPayloadAsFailed() {
        CollectionRunOutboxJpaEntity entity = pending();

        entity.markInvalidPayload(NOW);

        assertThat(entity.getPublishStatus())
                .isEqualTo(CollectionRunOutbox.PublishStatus.FAILED);
        assertThat(entity.getLastErrorMessage())
                .isEqualTo("INVALID_OUTBOX_PAYLOAD");
    }

    // 테스트용 최초 PENDING Outbox를 생성합니다.
    private CollectionRunOutboxJpaEntity pending() {
        return CollectionRunOutboxJpaEntity.pending(
                "event-id",
                mock(CollectionRunJpaEntity.class),
                "attempt-id",
                "COLLECTION_RUN_REQUESTED",
                JsonNodeFactory.instance.objectNode(),
                NOW
        );
    }
}
