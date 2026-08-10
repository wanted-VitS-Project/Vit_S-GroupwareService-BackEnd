package com.group3.vitamins.bidding.collectionrun.application.service;

import com.group3.vitamins.bidding.collectionrun.application.model.ClaimedCollectionRunOutbox;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunJobPublisherPort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunOutboxStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionRunOutboxPublishService")
class CollectionRunOutboxPublishServiceTest {

    private static final String LOCK_OWNER = "dispatcher-test";
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 10, 15, 30);

    @Mock
    private CollectionRunOutboxStorePort outboxStorePort;

    @Mock
    private CollectionRunJobPublisherPort jobPublisherPort;

    private CollectionRunOutboxPublishService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-10T06:30:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        service = new CollectionRunOutboxPublishService(
                outboxStorePort,
                jobPublisherPort,
                clock,
                300
        );
    }

    @Nested
    @DisplayName("발행 처리")
    class Publish {

        @Test
        @DisplayName("Redis 발행 성공 후 Outbox를 PUBLISHED 처리한다")
        void marksPublishedAfterRedisPublishSucceeds() {
            ClaimedCollectionRunOutbox outbox = claimedOutbox(1);
            when(outboxStorePort.claimPublishable(
                    LOCK_OWNER,
                    10,
                    NOW,
                    NOW.plusSeconds(300)
            )).thenReturn(List.of(outbox));

            int publishedCount = service.publishBatch(LOCK_OWNER, 10);

            assertThat(publishedCount).isEqualTo(1);
            verify(jobPublisherPort).publish(outbox);
            verify(outboxStorePort).markPublished(1L, LOCK_OWNER, NOW);
            verify(outboxStorePort, never()).markPublishFailed(
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.any(LocalDateTime.class)
            );
        }

        @Test
        @DisplayName("Redis 발행 실패 시 오류와 다음 재시도 시각을 기록한다")
        void schedulesRetryAfterRedisPublishFails() {
            ClaimedCollectionRunOutbox outbox = claimedOutbox(2);
            when(outboxStorePort.claimPublishable(
                    LOCK_OWNER,
                    10,
                    NOW,
                    NOW.plusSeconds(300)
            )).thenReturn(List.of(outbox));
            doThrow(new IllegalStateException("redis unavailable"))
                    .when(jobPublisherPort).publish(outbox);

            int publishedCount = service.publishBatch(LOCK_OWNER, 10);

            assertThat(publishedCount).isZero();
            verify(outboxStorePort).markPublishFailed(
                    1L,
                    LOCK_OWNER,
                    "REDIS_PUBLISH_FAILED",
                    NOW.plusSeconds(30)
            );
            verify(outboxStorePort, never()).markPublished(
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.any(LocalDateTime.class)
            );
        }
    }

    @Nested
    @DisplayName("입력 검증")
    class Validation {

        @Test
        @DisplayName("잠금 소유자가 비어 있으면 외부 작업을 시작하지 않는다")
        void rejectsBlankLockOwner() {
            assertThatThrownBy(() -> service.publishBatch(" ", 10))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(outboxStorePort, jobPublisherPort);
        }

        @Test
        @DisplayName("배치 크기가 허용 범위를 벗어나면 외부 작업을 시작하지 않는다")
        void rejectsInvalidBatchSize() {
            assertThatThrownBy(() -> service.publishBatch(LOCK_OWNER, 101))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(outboxStorePort, jobPublisherPort);
        }
    }

    // Dispatcher가 Redis에 전달할 최소 작업 정보를 만듭니다.
    private ClaimedCollectionRunOutbox claimedOutbox(int publishAttemptCount) {
        return new ClaimedCollectionRunOutbox(
                1L,
                11L,
                21L,
                31L,
                "event-1",
                "BIDDING_COLLECTION_RUN_REQUESTED",
                "attempt-1",
                0,
                publishAttemptCount
        );
    }
}
