package com.group3.vitamins.bidding.collectionrun.infrastructure.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunJob;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunJobResult;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunFailureType;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunJobHandlerPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Range;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "bidding.collection.worker",
        name = "enabled",
        havingValue = "true"
)
public class RedisCollectionRunJobConsumer {

    private static final int MAX_RETRY_COUNT = 3;

    private final StringRedisTemplate redisTemplate;
    private final CollectionRunJobHandlerPort jobHandlerPort;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    @Value("${bidding.collection.stream-key:bidding:collection:jobs}")
    private String streamKey;

    @Value("${bidding.collection.dlq-stream-key:bidding:collection:jobs:dlq}")
    private String dlqStreamKey;

    @Value("${bidding.collection.retry-key:bidding:collection:jobs:retry}")
    private String retryKey;

    @Value("${bidding.collection.worker.group:bidding-collection-workers}")
    private String consumerGroup;

    @Value("${bidding.collection.worker.consumer:bidding-collection-worker-local}")
    private String consumerName;

    @Value("${bidding.collection.worker.batch-size:10}")
    private int batchSize;

    @Value("${bidding.collection.worker.pending-min-idle-ms:60000}")
    private long pendingMinIdleMs;

    // 최초 실행 시 Redis Stream과 Consumer Group을 멱등하게 준비합니다.
    @PostConstruct
    public void ensureConsumerGroup() {
        try {
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                byte[] rawKey = streamKey.getBytes(StandardCharsets.UTF_8);
                connection.streamCommands().xGroupCreate(
                        rawKey,
                        consumerGroup,
                        ReadOffset.from("0-0"),
                        true
                );
                return null;
            });
        } catch (DataAccessException exception) {
            if (!containsBusyGroup(exception)) {
                throw exception;
            }
        }
    }

    // Consumer Group에서 새 작업을 읽어 처리 결과에 따라 ACK, 재시도 또는 DLQ로 분기합니다.
    @Scheduled(
            fixedDelayString =
                    "${bidding.collection.worker.poll-delay-ms:1000}"
    )
    public void consume() {
        recoverIdlePendingMessages();

        List<MapRecord<String, Object, Object>> records =
                redisTemplate.opsForStream().read(
                        Consumer.from(consumerGroup, consumerName),
                        StreamReadOptions.empty().count(batchSize),
                        StreamOffset.create(
                                streamKey,
                                ReadOffset.lastConsumed()
                        )
                );

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            process(record);
        }
    }

    private void process(MapRecord<String, Object, Object> record) {
        try {
            CollectionRunJob job = parse(record);
            CollectionRunJobResult result = jobHandlerPort.handle(job);

            switch (result.outcome()) {
                case SUCCESS -> acknowledge(record);
                case RETRYABLE_FAILURE -> handleRetryableFailure(
                        record,
                        job,
                        result.failureType(),
                        result.retryTarget()
                );
                case PERMANENT_FAILURE -> moveToDlqAndAcknowledge(
                        record,
                        job,
                        result.failureType()
                );
            }
        } catch (IllegalArgumentException exception) {
            moveMalformedMessageToDlq(record);
        } catch (RuntimeException exception) {
            log.error(
                    "Bidding collection job processing failed unexpectedly. recordId={}, errorType={}",
                    record.getId(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    private void handleRetryableFailure(
            MapRecord<String, Object, Object> record,
            CollectionRunJob job,
            CollectionRunFailureType failureType,
            CollectionRequestCombination retryTarget
    ) {
        if (job.retryCount() >= MAX_RETRY_COUNT) {
            moveToDlqAndAcknowledge(record, job, failureType);
            return;
        }

        CollectionRunJob retryJob = new CollectionRunJob(
                job.runId(),
                job.conditionId(),
                job.companyId(),
                UUID.randomUUID().toString(),
                job.retryCount() + 1,
                retryTarget
        );

        boolean scheduled = Boolean.TRUE.equals(
                redisTemplate.opsForZSet().add(
                        retryKey,
                        serializeJob(retryJob),
                        Instant.now(clock).plus(retryDelay(job.retryCount()))
                                .toEpochMilli()
                )
        );
        if (!scheduled) {
            throw new IllegalStateException("재시도 예약에 실패했습니다.");
        }
        acknowledge(record);
    }

    private void moveToDlqAndAcknowledge(
            MapRecord<String, Object, Object> record,
            CollectionRunJob job,
            CollectionRunFailureType failureType
    ) {
        Map<String, String> fields = jobFields(job);
        fields.put("errorType", safeFailureType(failureType).name());

        redisTemplate.opsForStream().add(
                MapRecord.create(dlqStreamKey, fields)
        );
        acknowledge(record);
    }

    private void moveMalformedMessageToDlq(
            MapRecord<String, Object, Object> record
    ) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("recordId", record.getId().getValue());
        fields.put("errorType", "MALFORMED_MESSAGE");

        redisTemplate.opsForStream().add(
                MapRecord.create(dlqStreamKey, fields)
        );
        acknowledge(record);
    }

    private CollectionRunJob parse(
            MapRecord<String, Object, Object> record
    ) {
        Map<Object, Object> fields = record.getValue();
        return new CollectionRunJob(
                parseLong(fields, "runId"),
                parseLong(fields, "conditionId"),
                parseLong(fields, "companyId"),
                required(fields, "attemptId"),
                parseInt(fields, "retryCount"),
                parseRetryTarget(fields)
        );
    }

    private Map<String, String> jobFields(CollectionRunJob job) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("runId", String.valueOf(job.runId()));
        fields.put("conditionId", String.valueOf(job.conditionId()));
        fields.put("companyId", String.valueOf(job.companyId()));
        fields.put("attemptId", job.attemptId());
        fields.put("retryCount", String.valueOf(job.retryCount()));
        CollectionRequestCombination retryTarget = job.retryTarget();
        if (retryTarget != null) {
            fields.put("retryNoticeType", retryTarget.noticeType().name());
            putIfPresent(fields, "retryKeyword", retryTarget.keyword());
            putIfPresent(fields, "retryRegionCode", retryTarget.regionCode());
            putIfPresent(fields, "retryIndustryCode", retryTarget.industryCode());
            fields.put("retryPageNumber", String.valueOf(retryTarget.pageNumber()));
        }
        return fields;
    }

    private CollectionRequestCombination parseRetryTarget(
            Map<Object, Object> fields
    ) {
        String noticeType = optional(fields, "retryNoticeType");
        if (noticeType == null) {
            return null;
        }
        return new CollectionRequestCombination(
                BidNoticeType.valueOf(noticeType),
                optional(fields, "retryKeyword"),
                optional(fields, "retryRegionCode"),
                optional(fields, "retryIndustryCode"),
                parseInt(fields, "retryPageNumber")
        );
    }

    private void putIfPresent(
            Map<String, String> fields,
            String name,
            String value
    ) {
        if (value != null) {
            fields.put(name, value);
        }
    }

    private String optional(Map<Object, Object> fields, String name) {
        Object value = fields.get(name);
        return value == null || value.toString().isBlank()
                ? null
                : value.toString();
    }

    private void acknowledge(MapRecord<String, Object, Object> record) {
        redisTemplate.opsForStream().acknowledge(
                streamKey,
                consumerGroup,
                record.getId()
        );
    }

    private Long parseLong(Map<Object, Object> fields, String name) {
        try {
            return Long.valueOf(required(fields, name));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " 형식이 잘못되었습니다.");
        }
    }

    private int parseInt(Map<Object, Object> fields, String name) {
        try {
            return Integer.parseInt(required(fields, name));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " 형식이 잘못되었습니다.");
        }
    }

    private String required(Map<Object, Object> fields, String name) {
        Object value = fields.get(name);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(name + " 값이 없습니다.");
        }
        return value.toString();
    }

    private CollectionRunFailureType safeFailureType(
            CollectionRunFailureType failureType
    ) {
        return failureType == null
                ? CollectionRunFailureType.UNKNOWN_PROCESSING_ERROR
                : failureType;
    }

    // 처리 중 중단되어 PEL에 남은 메시지를 유휴 시간 이후 현재 Consumer가 회수합니다.
    private void recoverIdlePendingMessages() {
        PendingMessages pendingMessages = redisTemplate.opsForStream().pending(
                streamKey,
                consumerGroup,
                Range.unbounded(),
                batchSize
        );
        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        for (PendingMessage pending : pendingMessages) {
            if (pending.getElapsedTimeSinceLastDelivery().toMillis()
                    < pendingMinIdleMs) {
                continue;
            }
            List<MapRecord<String, Object, Object>> claimed =
                    redisTemplate.opsForStream().claim(
                            streamKey,
                            consumerGroup,
                            consumerName,
                            Duration.ofMillis(pendingMinIdleMs),
                            pending.getId()
                    );
            if (claimed != null) {
                claimed.forEach(this::process);
            }
        }
    }

    // 예약 시각이 지난 재시도 작업만 다시 원본 Stream으로 이동합니다.
    @Scheduled(fixedDelayString = "${bidding.collection.worker.retry-poll-delay-ms:1000}")
    public void dispatchDueRetries() {
        var dueJobs = redisTemplate.opsForZSet().rangeByScore(
                retryKey,
                0,
                Instant.now(clock).toEpochMilli(),
                0,
                batchSize
        );
        if (dueJobs == null) {
            return;
        }
        for (String serialized : dueJobs) {
            CollectionRunJob job = deserializeJob(serialized);
            redisTemplate.opsForStream().add(
                    MapRecord.create(streamKey, jobFields(job))
            );
            redisTemplate.opsForZSet().remove(retryKey, serialized);
        }
    }

    private Duration retryDelay(int currentRetryCount) {
        return switch (currentRetryCount) {
            case 0 -> Duration.ofSeconds(30);
            case 1 -> Duration.ofMinutes(2);
            default -> Duration.ofMinutes(10);
        };
    }

    // 구분자 충돌 없이 재시도 대상 조합 전체를 JSON으로 보존합니다.
    private String serializeJob(CollectionRunJob job) {
        try {
            return objectMapper.writeValueAsString(job);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "재시도 작업을 직렬화할 수 없습니다.",
                    exception
            );
        }
    }

    // 지연 저장소의 JSON을 조합 정보가 포함된 작업으로 복원합니다.
    private CollectionRunJob deserializeJob(String value) {
        try {
            return objectMapper.readValue(value, CollectionRunJob.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "재시도 작업 형식이 올바르지 않습니다.",
                    exception
            );
        }
    }

    private boolean containsBusyGroup(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage().contains("BUSYGROUP")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
