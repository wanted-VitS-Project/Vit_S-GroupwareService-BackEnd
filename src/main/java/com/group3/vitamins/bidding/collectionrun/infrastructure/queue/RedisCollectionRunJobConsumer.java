package com.group3.vitamins.bidding.collectionrun.infrastructure.queue;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunJob;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunJobResult;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunJobHandlerPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
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

    @Value("${bidding.collection.stream-key:bidding:collection:jobs}")
    private String streamKey;

    @Value("${bidding.collection.dlq-stream-key:bidding:collection:jobs:dlq}")
    private String dlqStreamKey;

    @Value("${bidding.collection.worker.group:bidding-collection-workers}")
    private String consumerGroup;

    @Value("${bidding.collection.worker.consumer:bidding-collection-worker-local}")
    private String consumerName;

    @Value("${bidding.collection.worker.batch-size:10}")
    private int batchSize;

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
                        result.errorType()
                );
                case PERMANENT_FAILURE -> moveToDlqAndAcknowledge(
                        record,
                        job,
                        result.errorType()
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
            String errorType
    ) {
        if (job.retryCount() >= MAX_RETRY_COUNT) {
            moveToDlqAndAcknowledge(record, job, errorType);
            return;
        }

        Map<String, String> retryFields = jobFields(
                new CollectionRunJob(
                        job.runId(),
                        job.conditionId(),
                        job.companyId(),
                        UUID.randomUUID().toString(),
                        job.retryCount() + 1
                )
        );

        redisTemplate.opsForStream().add(
                MapRecord.create(streamKey, retryFields)
        );
        acknowledge(record);
    }

    private void moveToDlqAndAcknowledge(
            MapRecord<String, Object, Object> record,
            CollectionRunJob job,
            String errorType
    ) {
        Map<String, String> fields = jobFields(job);
        fields.put("errorType", safeErrorType(errorType));

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
                parseInt(fields, "retryCount")
        );
    }

    private Map<String, String> jobFields(CollectionRunJob job) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("runId", String.valueOf(job.runId()));
        fields.put("conditionId", String.valueOf(job.conditionId()));
        fields.put("companyId", String.valueOf(job.companyId()));
        fields.put("attemptId", job.attemptId());
        fields.put("retryCount", String.valueOf(job.retryCount()));
        return fields;
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

    private String safeErrorType(String errorType) {
        if (errorType == null || errorType.isBlank()) {
            return "UNKNOWN_PROCESSING_ERROR";
        }
        return errorType.length() <= 100
                ? errorType
                : errorType.substring(0, 100);
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
