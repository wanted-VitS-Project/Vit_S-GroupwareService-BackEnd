package com.group3.vitamins.bidding.collectionrun.infrastructure.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunFailureType;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunJob;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunJobResult;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunJobHandlerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisCollectionRunJobConsumer")
@SuppressWarnings({"unchecked", "rawtypes"})
class RedisCollectionRunJobConsumerTest {

    private static final String STREAM = "bidding:collection:jobs";
    private static final String RETRY_KEY = "bidding:collection:jobs:retry";
    private static final String GROUP = "bidding-collection-workers";
    private static final String CONSUMER = "consumer-test";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private CollectionRunJobHandlerPort handlerPort;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RedisCollectionRunJobConsumer consumer;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-10T07:30:00Z"),
                ZoneOffset.UTC
        );
        consumer = new RedisCollectionRunJobConsumer(
                redisTemplate,
                handlerPort,
                clock,
                new ObjectMapper().findAndRegisterModules()
        );
        ReflectionTestUtils.setField(consumer, "streamKey", STREAM);
        ReflectionTestUtils.setField(consumer, "dlqStreamKey", STREAM + ":dlq");
        ReflectionTestUtils.setField(consumer, "retryKey", RETRY_KEY);
        ReflectionTestUtils.setField(consumer, "consumerGroup", GROUP);
        ReflectionTestUtils.setField(consumer, "consumerName", CONSUMER);
        ReflectionTestUtils.setField(consumer, "batchSize", 10);
        ReflectionTestUtils.setField(consumer, "pendingMinIdleMs", 60_000L);

        when(redisTemplate.<Object, Object>opsForStream())
                .thenReturn(streamOperations);
    }

    @Test
    @DisplayName("일시 실패 작업은 30초 뒤 재시도로 예약한 후 ACK한다")
    void schedulesRetryBeforeAcknowledging() throws Exception {
        MapRecord<String, Object, Object> record = retryTargetJobRecord("1-0", 0);
        when(streamOperations.pending(STREAM, GROUP, Range.unbounded(), 10))
                .thenReturn(new PendingMessages(GROUP, List.of()));
        when(streamOperations.read(
                any(Consumer.class),
                any(StreamReadOptions.class),
                any(StreamOffset[].class)
        )).thenReturn(List.of(record));
        when(handlerPort.handle(any(CollectionRunJob.class)))
                .thenReturn(CollectionRunJobResult.retryableFailure(
                        CollectionRunFailureType.TIMEOUT
                ));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.add(eq(RETRY_KEY), any(), anyDouble()))
                .thenReturn(true);

        consumer.consume();

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
        verify(zSetOperations).add(
                eq(RETRY_KEY),
                valueCaptor.capture(),
                scoreCaptor.capture()
        );
        CollectionRunJob retryJob = new ObjectMapper()
                .findAndRegisterModules()
                .readValue(valueCaptor.getValue(), CollectionRunJob.class);
        assertThat(retryJob.retryCount()).isEqualTo(1);
        assertThat(retryJob.retryTarget()).isEqualTo(
                new CollectionRequestCombination(
                        BidNoticeType.SERVICE,
                        "스마트시티|통합관제",
                        "11",
                        "6202",
                        2
                )
        );
        assertThat(scoreCaptor.getValue())
                .isEqualTo(Instant.parse("2026-08-10T07:30:30Z").toEpochMilli());
        verify(streamOperations).acknowledge(STREAM, GROUP, record.getId());
        verify(streamOperations, never()).add(any(MapRecord.class));
    }

    @Test
    @DisplayName("유휴 시간이 지난 PEL 메시지를 회수해 처리하고 ACK한다")
    void recoversIdlePendingMessage() {
        MapRecord<String, Object, Object> record = jobRecord("2-0", 0);
        PendingMessage pending = new PendingMessage(
                record.getId(),
                Consumer.from(GROUP, "stopped-consumer"),
                Duration.ofMinutes(2),
                1
        );
        when(streamOperations.pending(STREAM, GROUP, Range.unbounded(), 10))
                .thenReturn(new PendingMessages(GROUP, List.of(pending)));
        when(streamOperations.claim(
                STREAM,
                GROUP,
                CONSUMER,
                Duration.ofSeconds(60),
                record.getId()
        )).thenReturn(List.of(record));
        when(streamOperations.read(
                any(Consumer.class),
                any(StreamReadOptions.class),
                any(StreamOffset[].class)
        )).thenReturn(List.of());
        when(handlerPort.handle(any(CollectionRunJob.class)))
                .thenReturn(CollectionRunJobResult.success());

        consumer.consume();

        verify(handlerPort).handle(any(CollectionRunJob.class));
        verify(streamOperations).acknowledge(STREAM, GROUP, record.getId());
    }

    // Redis Stream에서 전달받는 최소 수집 작업 메시지를 만듭니다.
    private MapRecord<String, Object, Object> jobRecord(
            String recordId,
            int retryCount
    ) {
        return MapRecord.create(
                STREAM,
                Map.<Object, Object>of(
                        "runId", "1",
                        "conditionId", "2",
                        "companyId", "3",
                        "attemptId", "attempt-id",
                        "retryCount", String.valueOf(retryCount)
                )
        ).withId(RecordId.of(recordId));
    }

    // 재시도할 외부 API 요청 조합이 포함된 Redis 메시지를 만듭니다.
    private MapRecord<String, Object, Object> retryTargetJobRecord(
            String recordId,
            int retryCount
    ) {
        return MapRecord.create(
                STREAM,
                Map.<Object, Object>of(
                        "runId", "1",
                        "conditionId", "2",
                        "companyId", "3",
                        "attemptId", "attempt-id",
                        "retryCount", String.valueOf(retryCount),
                        "retryNoticeType", "SERVICE",
                        "retryKeyword", "스마트시티|통합관제",
                        "retryRegionCode", "11",
                        "retryIndustryCode", "6202",
                        "retryPageNumber", "2"
                )
        ).withId(RecordId.of(recordId));
    }
}
