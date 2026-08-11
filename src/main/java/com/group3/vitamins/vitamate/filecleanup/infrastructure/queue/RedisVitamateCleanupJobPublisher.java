package com.group3.vitamins.vitamate.filecleanup.infrastructure.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.vitamate.filecleanup.application.model.ClaimedVitamateCleanupOutbox;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateCleanupJobPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisVitamateCleanupJobPublisher
        implements VitamateCleanupJobPublisherPort {

    private static final String STREAM_KEY =
            "vitamate:chroma-cleanup:jobs";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Python worker가 처리할 ChromaDB 정리 작업을 Redis Stream에 발행합니다.
    @Override
    public void publish(ClaimedVitamateCleanupOutbox outbox) {
        Map<String, String> fields = Map.of(
                "cleanupJobId", String.valueOf(outbox.cleanupJobId()),
                "cleanupKey", outbox.cleanupKey(),
                "attemptId", outbox.attemptId(),
                "fileVersionIds", serializeFileVersionIds(outbox),
                "retryCount", String.valueOf(outbox.retryCount())
        );

        MapRecord<String, String, String> record =
                MapRecord.create(STREAM_KEY, fields);

        RecordId recordId =
                redisTemplate.opsForStream().add(record);

        if (recordId == null) {
            throw new IllegalStateException(
                    "Redis Stream이 정리 작업의 recordId를 반환하지 않았습니다."
            );
        }

        log.info(
                "Vitamate cleanup job published. cleanupJobId={}, attemptId={}, streamKey={}, recordId={}",
                outbox.cleanupJobId(),
                outbox.attemptId(),
                STREAM_KEY,
                recordId
        );
    }

    // 파일 버전 ID 목록을 Redis 메시지에 넣을 JSON 배열 문자열로 변환합니다.
    private String serializeFileVersionIds(
            ClaimedVitamateCleanupOutbox outbox
    ) {
        try {
            return objectMapper.writeValueAsString(
                    outbox.fileVersionIds()
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "정리 대상 파일 버전 ID 직렬화에 실패했습니다.",
                    exception
            );
        }
    }
}