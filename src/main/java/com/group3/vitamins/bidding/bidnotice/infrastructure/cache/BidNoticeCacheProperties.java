package com.group3.vitamins.bidding.bidnotice.infrastructure.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "bidding.notice-cache")
public record BidNoticeCacheProperties(Duration ttl) {

    public BidNoticeCacheProperties {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("bidding.notice-cache.ttl must be positive");
        }
    }
}
