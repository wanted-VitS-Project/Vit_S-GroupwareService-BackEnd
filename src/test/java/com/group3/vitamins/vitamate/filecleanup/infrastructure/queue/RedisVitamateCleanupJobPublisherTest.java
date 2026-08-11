package com.group3.vitamins.vitamate.filecleanup.infrastructure.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.vitamate.filecleanup.application.model.ClaimedVitamateCleanupOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisVitamateCleanupJobPublisher")
class RedisVitamateCleanupJobPublisherTest {

    private static final String STREAM_KEY =
            "vitamate:chroma-cleanup:jobs";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, String, String> streamOperations;

    private RedisVitamateCleanupJobPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RedisVitamateCleanupJobPublisher(
                redisTemplate,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("Python worker 계약에 맞는 정리 작업을 Redis Stream에 발행한다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void publishesCleanupJobWithExpectedFields() {
        ClaimedVitamateCleanupOutbox outbox = outbox();

        when(redisTemplate.<String, String>opsForStream())
                .thenReturn(streamOperations);
        when(streamOperations.add(any()))
                .thenReturn(RecordId.of("1-0"));

        publisher.publish(outbox);

        ArgumentCaptor<MapRecord> recordCaptor =
                ArgumentCaptor.forClass(MapRecord.class);

        verify(streamOperations).add(recordCaptor.capture());

        MapRecord<String, String, String> record =
                recordCaptor.getValue();

        assertThat(record.getStream()).isEqualTo(STREAM_KEY);
        assertThat(record.getValue()).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        "cleanupJobId", "31",
                        "cleanupKey",
                        "550e8400-e29b-41d4-a716-446655440000",
                        "attemptId",
                        "91f3c9c4-27dd-48e7-af1b-732b69eac214",
                        "fileVersionIds", "[101,102]",
                        "retryCount", "0"
                )
        );
    }

    @Test
    @DisplayName("Redis가 recordId를 반환하지 않으면 발행 실패로 처리한다")
    void rejectsMissingRedisRecordId() {
        when(redisTemplate.<String, String>opsForStream())
                .thenReturn(streamOperations);
        when(streamOperations.add(any()))
                .thenReturn(null);

        assertThatThrownBy(() -> publisher.publish(outbox()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("recordId");
    }

    @Test
    @DisplayName("파일 버전 ID 직렬화에 실패하면 Redis를 호출하지 않는다")
    void doesNotPublishWhenSerializationFails() throws Exception {
        ObjectMapper failingObjectMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value)
                    throws JsonProcessingException {
                throw new JsonProcessingException(
                        "serialization failed"
                ) {
                };
            }
        };

        RedisVitamateCleanupJobPublisher failingPublisher =
                new RedisVitamateCleanupJobPublisher(
                        redisTemplate,
                        failingObjectMapper
                );

        assertThatThrownBy(() ->
                failingPublisher.publish(outbox())
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("직렬화");

        verify(redisTemplate, never()).opsForStream();
    }

    // Python worker 계약과 동일한 테스트용 Cleanup Outbox를 생성합니다.
    private ClaimedVitamateCleanupOutbox outbox() {
        return new ClaimedVitamateCleanupOutbox(
                41L,
                31L,
                "event-41",
                "CHROMA_VECTOR_DELETE_REQUESTED",
                "550e8400-e29b-41d4-a716-446655440000",
                "91f3c9c4-27dd-48e7-af1b-732b69eac214",
                List.of(101L, 102L),
                0,
                1
        );
    }
}