package com.group3.vitamins.vitamate.fileindex.infrastructure.queue;

import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexJobPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

// Redis Streams에 비타메이트 파일 인덱싱 작업 메시지를 발행하는 어댑터
// (RedisVitamateAnalysisJobPublisher와 동일 패턴, 다른 stream key)
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisVitamateFileIndexJobPublisher implements VitamateFileIndexJobPublisherPort {

    private static final String STREAM_KEY = "vitamate:file-index:jobs";

    private final StringRedisTemplate redisTemplate;

    // 파일 인덱싱 작업을 Redis Stream에 발행한다.
    @Override
    public void publish(FileIndexJob job) {
        Map<String, String> fields = Map.of(
                "fileVersionId", String.valueOf(job.fileVersionId()),
                "retryCount", String.valueOf(job.retryCount()),
                "createdAt", job.createdAt().toString()
        );

        MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, fields);
        RecordId recordId = redisTemplate.opsForStream().add(record);

        log.info("Vitamate file index job published. fileVersionId={}, streamKey={}, recordId={}",
                job.fileVersionId(), STREAM_KEY, recordId);
    }
}