package com.group3.vitamins.bidding.bidreview.infrastructure.queue;

import com.group3.vitamins.bidding.bidreview.application.model.ClaimedBidReviewOutbox;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewJobPublisherPort;
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
public class RedisBidReviewJobPublisher
        implements BidReviewJobPublisherPort {

    private final StringRedisTemplate redisTemplate;

    @Value("${bidding.review.stream-key:bidding:review:jobs}")
    private String streamKey;

    // Python worker가 처리할 최소한의 작업 식별 정보만 발행합니다.
    @Override
    public void publish(ClaimedBidReviewOutbox outbox) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("reviewId", String.valueOf(outbox.reviewId()));
        fields.put("companyId", String.valueOf(outbox.companyId()));
        fields.put("attemptId", outbox.attemptId());
        fields.put("retryCount", String.valueOf(outbox.retryCount()));

        MapRecord<String, String, String> record = MapRecord.create(streamKey, fields);

        RecordId recordId = redisTemplate.opsForStream().add(record);

        if (recordId == null) {
            throw new IllegalStateException("입찰 문서 검토 Redis 메시지 ID를 발급받지 못했습니다.");
        }

        log.info(
                "Bid review job published. reviewId={}, attemptId={}, retryCount={}, recordId={}",
                outbox.reviewId(),
                outbox.attemptId(),
                outbox.retryCount(),
                recordId
        );
    }
}