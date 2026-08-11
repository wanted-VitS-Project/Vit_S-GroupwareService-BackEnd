package com.group3.vitamins.bidding.bidnotice.infrastructure.cache;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BidNoticeCacheInvalidationRetryQueue {

    private final ConcurrentHashMap<Long, Long> pendingGenerations =
            new ConcurrentHashMap<>();

    // 같은 회사의 연속 실패를 하나의 항목으로 합치되 새 변경 세대는 보존합니다.
    public void enqueue(Long companyId) {
        pendingGenerations.merge(companyId, 1L, Long::sum);
    }

    // 한 번의 스케줄에서 처리할 회사별 현재 세대를 제한된 개수만큼 반환합니다.
    public List<RetryEntry> snapshot(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return pendingGenerations.entrySet().stream()
                .limit(limit)
                .map(entry -> new RetryEntry(entry.getKey(), entry.getValue()))
                .toList();
    }

    // 재시도 중 새 변경이 없었던 항목만 제거해 동시 등록을 잃지 않습니다.
    public void removeIfUnchanged(RetryEntry entry) {
        pendingGenerations.remove(entry.companyId(), entry.generation());
    }

    public int size() {
        return pendingGenerations.size();
    }

    public record RetryEntry(Long companyId, Long generation) {
    }
}
