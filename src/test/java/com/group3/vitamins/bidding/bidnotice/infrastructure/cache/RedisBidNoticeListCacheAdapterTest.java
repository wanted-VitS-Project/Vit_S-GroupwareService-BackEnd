package com.group3.vitamins.bidding.bidnotice.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidnotice.application.query.SearchBidNoticesQuery;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeListResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisBidNoticeListCacheAdapterTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisBidNoticeListCacheAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        adapter = new RedisBidNoticeListCacheAdapter(
                redisTemplate,
                new ObjectMapper().findAndRegisterModules(),
                new BidNoticeCacheProperties(Duration.ofHours(24))
        );
    }

    @Test
    void returnsCacheMissWithCurrentCompanyVersion() {
        when(valueOperations.get("bidding:notices:version:10")).thenReturn("3");
        when(valueOperations.get(org.mockito.ArgumentMatchers.<String>argThat(key -> key.startsWith(
                "bidding:notices:data:10:v3:"
        )))).thenReturn(null);

        var lookup = adapter.lookup(10L, query());

        assertThat(lookup.version()).isEqualTo("3");
        assertThat(lookup.result()).isEmpty();
    }

    @Test
    void returnsCachedResultWhenCompanyVersionRemainsUnchanged() throws Exception {
        BidNoticeListResult cached = new BidNoticeListResult(List.of(), 0, 0, 0, 20);
        when(valueOperations.get("bidding:notices:version:10"))
                .thenReturn("3", "3");
        when(valueOperations.get(org.mockito.ArgumentMatchers.<String>argThat(key -> key.startsWith(
                "bidding:notices:data:10:v3:"
        )))).thenReturn(new ObjectMapper().findAndRegisterModules().writeValueAsString(cached));

        var lookup = adapter.lookup(10L, query());

        assertThat(lookup.version()).isEqualTo("3");
        assertThat(lookup.result()).contains(cached);
    }

    @Test
    void discardsCachedResultWhenCompanyVersionChangesDuringLookup() {
        when(valueOperations.get("bidding:notices:version:10"))
                .thenReturn("3", "4");
        when(valueOperations.get(org.mockito.ArgumentMatchers.<String>argThat(key -> key.startsWith(
                "bidding:notices:data:10:v3:"
        )))).thenReturn("{}");

        var lookup = adapter.lookup(10L, query());

        assertThat(lookup.version()).isEqualTo("4");
        assertThat(lookup.result()).isEmpty();
    }

    @Test
    void writesResultOnlyToLookupVersionWithConfiguredTtl() {
        BidNoticeListResult result = new BidNoticeListResult(
                List.of(), 0, 0, 0, 20
        );

        adapter.put(10L, query(), "4", result);

        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.<String>argThat(
                        key -> key.startsWith("bidding:notices:data:10:v4:")
                ),
                anyString(),
                eq(Duration.ofHours(24))
        );
    }

    @Test
    void usesDifferentCacheKeysForFavoriteAndNonFavoriteQueries() {
        // favorite 필터가 캐시 키에 반영되지 않으면, 관심 목록만 보는 조회가 전체 목록 캐시를
        // 그대로 돌려주는(또는 반대) 오염이 생긴다 - 이 테스트는 그 회귀를 잡기 위한 것이다.
        BidNoticeListResult result = new BidNoticeListResult(List.of(), 0, 0, 0, 20);
        SearchBidNoticesQuery favoriteQuery = new SearchBidNoticesQuery(
                null, null, null, null, null, null,
                "smart-city", "COLLECTED", true, "ANNOUNCED_DESC",
                0, 20, "EMP001", "ADMIN"
        );

        adapter.put(10L, query(), "4", result);
        adapter.put(10L, favoriteQuery, "4", result);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations, times(2)).set(keyCaptor.capture(), anyString(), eq(Duration.ofHours(24)));
        assertThat(keyCaptor.getAllValues().get(0)).isNotEqualTo(keyCaptor.getAllValues().get(1));
    }

    @Test
    void incrementsOnlyTargetCompanyVersion() {
        assertThat(adapter.invalidate(10L)).isTrue();

        verify(valueOperations).increment("bidding:notices:version:10");
        verify(valueOperations, never()).increment("bidding:notices:version:11");
    }

    @Test
    void fallsBackToUnavailableWhenRedisLookupFails() {
        when(valueOperations.get("bidding:notices:version:10"))
                .thenThrow(new IllegalStateException("redis unavailable"));

        var lookup = adapter.lookup(10L, query());

        assertThat(lookup.version()).isNull();
        assertThat(lookup.result()).isEmpty();
    }

    @Test
    void doesNotFailRequestWhenRedisInvalidationFails() {
        when(valueOperations.increment("bidding:notices:version:10"))
                .thenThrow(new IllegalStateException("redis unavailable"));

        assertThat(adapter.invalidate(10L)).isFalse();
    }

    private SearchBidNoticesQuery query() {
        return new SearchBidNoticesQuery(
                null, null, null, null, null, null,
                "smart-city", "COLLECTED", null, "ANNOUNCED_DESC",
                0, 20, "EMP001", "ADMIN"
        );
    }
}
