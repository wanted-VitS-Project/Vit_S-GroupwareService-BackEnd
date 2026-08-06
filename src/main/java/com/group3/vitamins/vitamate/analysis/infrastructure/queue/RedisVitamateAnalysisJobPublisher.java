package com.group3.vitamins.vitamate.analysis.infrastructure.queue;

import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisJobPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

// Redis Streams에 비타메이트 분석 작업 메시지를 발행하는 어댑터
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisVitamateAnalysisJobPublisher implements VitamateAnalysisJobPublisherPort {

    private static final String STREAM_KEY = "vitamate:analysis:jobs";

    private final StringRedisTemplate redisTemplate;

    // 분석 작업을 Redis Stream에 발행한다.
    @Override
    public void publish(VitamateAnalysisJobPublisherPort.AnalysisJob job) {
        Map<String, String> fields = Map.of(
                "analysisId", String.valueOf(job.analysisId()),
                "attemptId", job.attemptId(),
                "retryCount", String.valueOf(job.retryCount()),
                "createdAt", job.createdAt().toString()
        );

        MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, fields);
        RecordId recordId = redisTemplate.opsForStream().add(record);

        log.info("Vitamate analysis job published. analysisId={}, attemptId={}, streamKey={}, recordId={}",
                job.analysisId(), job.attemptId(), STREAM_KEY, recordId);
    }
}
