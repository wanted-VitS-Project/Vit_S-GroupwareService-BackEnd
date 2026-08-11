package com.group3.vitamins.bidding.collectionrun.infrastructure.queue;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTaskFailure;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunTaskDlqPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RedisCollectionRunTaskDlqAdapter implements CollectionRunTaskDlqPort {

    private final StringRedisTemplate redisTemplate;
    private final String taskDlqStreamKey;

    public RedisCollectionRunTaskDlqAdapter(
            StringRedisTemplate redisTemplate,
            @Value("${bidding.collection.task-dlq-stream-key:bidding:collection:tasks:dlq}")
            String taskDlqStreamKey
    ) {
        this.redisTemplate = redisTemplate;
        this.taskDlqStreamKey = taskDlqStreamKey;
    }

    // 원문 응답이나 인증정보 없이 실패 원인과 요청 조합만 Redis Stream에 저장합니다.
    @Override
    public void publish(CollectionRunTaskFailure failure) {
        CollectionRequestCombination target = failure.target();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("dedupKey", failure.taskId() + ":" + failure.attemptId());
        fields.put("runId", String.valueOf(failure.runId()));
        fields.put("taskId", String.valueOf(failure.taskId()));
        fields.put("companyId", String.valueOf(failure.companyId()));
        fields.put("attemptId", failure.attemptId());
        fields.put("retryCount", String.valueOf(failure.retryCount()));
        fields.put("errorType", failure.failureType().name());
        fields.put("noticeType", target.noticeType().name());
        putIfPresent(fields, "keyword", target.keyword());
        putIfPresent(fields, "regionCode", target.regionCode());
        putIfPresent(fields, "industryCode", target.industryCode());
        fields.put("pageNumber", String.valueOf(target.pageNumber()));

        redisTemplate.opsForStream().add(MapRecord.create(taskDlqStreamKey, fields));
    }

    private void putIfPresent(Map<String, String> fields, String name, String value) {
        if (value != null) {
            fields.put(name, value);
        }
    }
}
