package com.group3.vitamins.bidding.collectionrun.infrastructure.queue;

import com.group3.vitamins.bidding.collectionrun.application.model.ClaimedCollectionRunOutbox;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunJobPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCollectionRunJobPublisher
        implements CollectionRunJobPublisherPort {

    private final StringRedisTemplate redisTemplate;

    @Value("${bidding.collection.stream-key:bidding:collection:jobs}")
    private String streamKey;

    // Spring Worker가 처리할 최소 수집 작업 정보를 Redis Stream에 발행합니다.
    @Override
    public void publish(ClaimedCollectionRunOutbox outbox) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("runId", String.valueOf(outbox.runId()));
        fields.put("conditionId", String.valueOf(outbox.conditionId()));
        fields.put("companyId", String.valueOf(outbox.companyId()));
        fields.put("attemptId", outbox.attemptId());
        fields.put("retryCount", String.valueOf(outbox.retryCount()));

        MapRecord<String, String, String> record =
                MapRecord.create(streamKey, fields);

        RecordId recordId = redisTemplate.opsForStream().add(record);

        if (recordId == null) {
            throw new IllegalStateException(
                    "입찰 수집 Redis Stream 메시지 ID를 발급받지 못했습니다."
            );
        }

        log.info(
                "Bidding collection job published. runId={}, attemptId={}, retryCount={}, recordId={}",
                outbox.runId(),
                outbox.attemptId(),
                outbox.retryCount(),
                recordId
        );
    }
}
