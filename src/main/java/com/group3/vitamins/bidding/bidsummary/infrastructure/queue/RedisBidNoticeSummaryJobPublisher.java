package com.group3.vitamins.bidding.bidsummary.infrastructure.queue;

import com.group3.vitamins.bidding.bidsummary.application.model.ClaimedBidNoticeSummaryOutbox;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryJobPublisherPort;
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
public class RedisBidNoticeSummaryJobPublisher
        implements BidNoticeSummaryJobPublisherPort {

    private final StringRedisTemplate redisTemplate;

    @Value("${bidding.summary.stream-key:bidding:summary:jobs}")
    private String streamKey;

    // Python worker가 처리할 최소한의 작업 식별 정보만 발행합니다.
    @Override
    public void publish(ClaimedBidNoticeSummaryOutbox outbox) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("summaryId", String.valueOf(outbox.summaryId()));
        fields.put("companyId", String.valueOf(outbox.companyId()));
        fields.put("attemptId", outbox.attemptId());
        fields.put("retryCount", String.valueOf(outbox.retryCount()));

        MapRecord<String, String, String> record =
                MapRecord.create(streamKey, fields);

        RecordId recordId = redisTemplate.opsForStream().add(record);

        if (recordId == null) {
            throw new IllegalStateException(
                    "입찰 AI 요약 Redis 메시지 ID를 발급받지 못했습니다."
            );
        }

        log.info(
                "Bidding summary job published. summaryId={}, attemptId={}, retryCount={}, recordId={}",
                outbox.summaryId(),
                outbox.attemptId(),
                outbox.retryCount(),
                recordId
        );
    }
}